package com.aituan.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import com.aituan.common.api.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StationMessagePublisherTest {

  @Autowired private StationMessagePublisher stationMessagePublisher;
  @Autowired private MessageRepository messageRepository;
  @Autowired private MessageService messageService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("delete from support_station_message where user_id = ?", 99002L);
    TestAuthSupport.clear();
  }

  @Test
  void publishedMessageAppearsInUserStationMessages() {
    stationMessagePublisher.order(
        99002L,
        "券码已生成",
        "测试门店的订单券码已生成，请到店出示核销。",
        "券码",
        88001L);
    TestAuthSupport.loginAsUser(99002L, 99002L);

    PageResponse<MessageView> page = messageService.listMessages(null, 1, 20);

    assertThat(messageRepository.countUnreadMessages(99002L)).isEqualTo(1);
    assertThat(page.list())
        .singleElement()
        .satisfies(message -> {
          assertThat(message.type()).isEqualTo("order");
          assertThat(message.title()).isEqualTo("券码已生成");
          assertThat(message.badgeText()).isEqualTo("券码");
          assertThat(message.unread()).isTrue();
          assertThat(message.relatedOrderId()).isEqualTo(88001L);
          assertThat(message.relatedTargetType()).isEqualTo("order");
          assertThat(message.relatedTargetId()).isEqualTo(88001L);
        });
  }

  @Test
  void longMessageFieldsAreTrimmedBeforeInsert() {
    stationMessagePublisher.support(
        99002L,
        "标题".repeat(80),
        "内容".repeat(300),
        null,
        77001L,
        "客服".repeat(30));
    TestAuthSupport.loginAsUser(99002L, 99002L);

    MessageView message = messageService.listMessages(null, 1, 20).list().get(0);

    assertThat(message.title()).hasSize(120);
    assertThat(message.content()).hasSize(500);
    assertThat(message.badgeText()).hasSize(40);
    assertThat(message.relatedTargetType()).isEqualTo("support_session");
    assertThat(message.relatedTargetId()).isEqualTo(77001L);
  }
}
