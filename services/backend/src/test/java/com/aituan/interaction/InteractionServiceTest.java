package com.aituan.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import org.junit.jupiter.api.AfterEach;
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
}
