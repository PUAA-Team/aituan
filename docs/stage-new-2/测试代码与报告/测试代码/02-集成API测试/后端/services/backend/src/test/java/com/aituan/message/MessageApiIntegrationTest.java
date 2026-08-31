package com.aituan.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanup() {
    jdbcTemplate.update("delete from support_station_message where title like '批量消息%' or title like '他人批量消息%'");
  }

  @Test
  void batchReadAndUnreadShouldOnlyAffectCurrentUserMessages() throws Exception {
    long firstId = insertMessage(1L, "批量消息已读1", "unread");
    long secondId = insertMessage(1L, "批量消息已读2", "unread");
    long foreignId = insertMessage(99003L, "他人批量消息已读", "unread");
    String token = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(patch("/api/app/message/station/batch-read"), token)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"messageIds":[%d,%d,%d,%d]}
                """.formatted(firstId, secondId, secondId, foreignId)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    assertThat(readStatus(firstId)).isEqualTo("read");
    assertThat(readStatus(secondId)).isEqualTo("read");
    assertThat(readStatus(foreignId)).isEqualTo("unread");

    mockMvc.perform(ApiTestSupport.bearer(patch("/api/app/message/station/batch-unread"), token)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"messageIds":[%d,%d]}
                """.formatted(firstId, foreignId)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    assertThat(readStatus(firstId)).isEqualTo("unread");
    assertThat(readStatus(secondId)).isEqualTo("read");
    assertThat(readStatus(foreignId)).isEqualTo("unread");
  }

  @Test
  void batchDeleteShouldSoftDeleteOnlyCurrentUserMessages() throws Exception {
    long currentUserId = insertMessage(1L, "批量消息删除", "unread");
    long foreignId = insertMessage(99003L, "他人批量消息删除", "unread");
    String token = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(patch("/api/app/message/station/batch-delete"), token)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"messageIds":[%d,%d]}
                """.formatted(currentUserId, foreignId)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    assertThat(isDeleted(currentUserId)).isEqualTo(1);
    assertThat(isDeleted(foreignId)).isZero();
  }

  @Test
  void emptyBatchRequestShouldReturnBadRequest() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            patch("/api/app/message/station/batch-read"),
            ApiTestSupport.userToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"messageIds":[]}
                """))
        .andExpect(ApiTestSupport.badRequest());
  }

  private long insertMessage(long userId, String title, String readStatus) {
    jdbcTemplate.update(
        """
        insert into support_station_message(user_id, message_type, title, content, badge_text, read_status)
        values (?, 'system', ?, '批量操作测试', '测试', ?)
        """,
        userId,
        title,
        readStatus);
    return jdbcTemplate.queryForObject(
        "select id from support_station_message where title = ?",
        Long.class,
        title);
  }

  private String readStatus(long messageId) {
    return jdbcTemplate.queryForObject(
        "select read_status from support_station_message where id = ?",
        String.class,
        messageId);
  }

  private int isDeleted(long messageId) {
    Integer value = jdbcTemplate.queryForObject(
        "select is_deleted from support_station_message where id = ?",
        Integer.class,
        messageId);
    return value == null ? 0 : value;
  }
}
