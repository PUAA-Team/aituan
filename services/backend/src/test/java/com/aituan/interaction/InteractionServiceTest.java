package com.aituan.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aituan.TestAuthSupport;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 评价回复幂等性测试：商家对同一评价重复回复，不抛异常且只保留首次回复。 */
@SpringBootTest
@ActiveProfiles("test")
class InteractionServiceTest {

  @Autowired private InteractionService interactionService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetReviewFixtures() {
    jdbcTemplate.update("delete from review_report where id > 3");
    jdbcTemplate.update("delete from review_record where id >= 9900");
    jdbcTemplate.update("delete from order_main where id >= 9900");
    jdbcTemplate.update(
        """
        update review_report
        set status = 'submitted', handled_by = null, handled_at = null, is_deleted = 0
        where id = 1
        """);
    jdbcTemplate.update(
        """
        update review_report
        set status = 'handled', handled_by = 3, handled_at = current_timestamp, is_deleted = 0
        where id in (2, 3)
        """);
    jdbcTemplate.update(
        "update review_record set reported_count = 1, status = 'published' where id = 9009");
    jdbcTemplate.update(
        "update review_record set reported_count = 2, status = 'hidden' where id = 9017");
  }

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void merchantReplyShouldBeIdempotent() {
    // 商家 1（账号 id=2，门店 1）回复 review 9017（store_id=1）
    TestAuthSupport.loginAsMerchant(2L);

    ReviewView first = interactionService.merchantReply(
        9017L, new MerchantReviewReplyRequest("感谢您的反馈，我们将提升出餐速度"));
    assertThat(first.replied()).isTrue();
    assertThat(first.replyContent()).isEqualTo("感谢您的反馈，我们将提升出餐速度");

    // 二次回复应当幂等：返回首次的内容，不抛异常
    ReviewView second = interactionService.merchantReply(
        9017L, new MerchantReviewReplyRequest("第二次回复应被忽略"));
    assertThat(second.replyContent()).isEqualTo("感谢您的反馈，我们将提升出餐速度");

    // DB 中仅有一条 review_reply
    Integer count = jdbcTemplate.queryForObject(
        "select count(1) from review_reply where review_id = ? and is_deleted = 0",
        Integer.class, 9017L);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void reportReviewStoresEvidenceImages() {
    TestAuthSupport.loginAsUser(1L, 1L);
    jdbcTemplate.update("update review_report set status = 'handled' where id = 1");
    jdbcTemplate.update("update review_record set reported_count = 0 where id = 9009");

    ReviewReportView report = interactionService.reportReview(
        9009L,
        new ReviewReportRequest(
            "图片证据",
            "截图可以证明问题",
            java.util.List.of("/api/common/files/report/demo.png")));

    String evidence = jdbcTemplate.queryForObject(
        "select evidence_urls from review_report where id = ?",
        String.class,
        report.reportId());
    assertThat(evidence).contains("/api/common/files/report/demo.png");
  }

  @Test
  void hiddenReviewCannotBeReported() {
    TestAuthSupport.loginAsUser(1L, 1L);

    assertThatThrownBy(() -> interactionService.reportReview(
        9017L,
        new ReviewReportRequest("内容不实", "隐藏评价不能继续举报", java.util.List.of())))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  void duplicateActiveReportIsIdempotentAndDoesNotIncrementCount() {
    TestAuthSupport.loginAsUser(1L, 1L);

    ReviewReportView report = interactionService.reportReview(
        9009L,
        new ReviewReportRequest("内容不实", "重复提交同一举报", java.util.List.of()));

    assertThat(report.reportId()).isEqualTo(1L);
    Integer activeReports = jdbcTemplate.queryForObject(
        """
        select count(1)
        from review_report
        where review_id = ? and reporter_user_id = ? and status = 'submitted' and is_deleted = 0
        """,
        Integer.class,
        9009L,
        1L);
    Integer reportedCount = jdbcTemplate.queryForObject(
        "select reported_count from review_record where id = ?",
        Integer.class,
        9009L);
    assertThat(activeReports).isEqualTo(1);
    assertThat(reportedCount).isEqualTo(1);
  }

  @Test
  void adminReportedFilterUsesOnlyActiveReports() {
    TestAuthSupport.loginAsAdmin(3L);

    var page = interactionService.adminReviews(null, true, 1, 20);

    assertThat(page.list()).anySatisfy(review -> assertThat(review.id()).isEqualTo(9009L));
    assertThat(page.list()).noneSatisfy(review -> assertThat(review.id()).isEqualTo(9017L));
  }

  @Test
  void adminAuditHandlesReportsAndRemovesReviewFromReportedFilter() {
    TestAuthSupport.loginAsAdmin(3L);

    ReviewView audited = interactionService.adminAudit(
        9009L, new AdminReviewAuditRequest("pass", "举报不成立"));

    Integer activeReports = jdbcTemplate.queryForObject(
        "select count(1) from review_report where review_id = ? and status = 'submitted' and is_deleted = 0",
        Integer.class,
        9009L);
    Integer reportedCount = jdbcTemplate.queryForObject(
        "select reported_count from review_record where id = ?",
        Integer.class,
        9009L);
    var reportedPage = interactionService.adminReviews(null, true, 1, 20);

    assertThat(audited.reportReasons()).isNull();
    assertThat(activeReports).isZero();
    assertThat(reportedCount).isZero();
    assertThat(reportedPage.list()).noneSatisfy(review -> assertThat(review.id()).isEqualTo(9009L));
  }

  @Test
  void submitReviewRejectsMissingRatingAsBusinessError() {
    TestAuthSupport.loginAsUser(1L, 1L);
    jdbcTemplate.update(
        """
        insert into order_main(id, order_no, user_id, store_id, store_name, order_type, title,
                               display_status, payment_status, fulfillment_status,
                               amount, delivery_fee, discount_amount, payable_amount)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        9901L, "AT202606049901", 1L, 1L, "塔斯汀中国汉堡", "takeaway", "空评分测试订单",
        "used", "paid", "completed",
        18.80, 4.00, 0.00, 22.80);

    assertThatThrownBy(() -> interactionService.submitReview(
        9901L,
        new ReviewCreateRequest(null, "内容正常但评分缺失", java.util.List.of(), java.util.List.of())))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test
  void merchantReportedFilterUsesReportedCountInsteadOfStatus() {
    TestAuthSupport.loginAsMerchant(30L);

    var page = interactionService.merchantReviews("reported", null, 1, 20);

    assertThat(page.list())
        .allSatisfy(review -> assertThat(review.reportedCount()).isGreaterThan(0));
    assertThat(page.list()).anySatisfy(review -> assertThat(review.id()).isEqualTo(9009L));
  }
}
