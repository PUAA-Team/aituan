package com.aituan.engagementplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

@SpringBootTest
class EngagementPlatformServiceApplicationTest {
  private static final Set<String> OWNED_TABLES=Set.of(
      "review_record","review_reply","review_helpful","review_report","review_audit_log",
      "support_session","support_message","merchant_support_auto_reply_rule",
      "complaint_ticket","complaint_log","ai_assistant_conversation","ai_assistant_message",
      "platform_announcement","sys_config","sys_dict","sys_request_log","sys_audit_log","file_asset");
  @Autowired JdbcTemplate jdbc;

  @Test void contextLoadsWithExactlyEighteenOwnedBusinessTables(){
    List<String> tables=jdbc.execute((ConnectionCallback<List<String>>) connection -> {
      List<String> names = new ArrayList<>();
      try (var result = connection.getMetaData().getTables(
          connection.getCatalog(), connection.getSchema(), "%", new String[] {"TABLE"})) {
        while (result.next()) names.add(result.getString("TABLE_NAME").toLowerCase());
      }
      return names;
    }).stream().filter(name->!name.equals("flyway_schema_history")).toList();
    assertThat(tables).containsExactlyInAnyOrderElementsOf(OWNED_TABLES);
  }
}
