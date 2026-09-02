package com.aituan.identity.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class IdentityFlywayH2MigrationTest {
  private static final List<String> EXPECTED_TABLES = List.of(
      "iam_account",
      "iam_verification_code",
      "iam_role",
      "iam_permission",
      "iam_account_role",
      "iam_role_permission",
      "user_profile",
      "user_address",
      "user_favorite",
      "member_level",
      "member_growth_log",
      "member_weekly_coupon_rule",
      "member_weekly_coupon_batch",
      "member_weekly_coupon_issue",
      "coupon_template",
      "user_coupon",
      "support_station_message");

  @Test
  void emptyH2DatabaseShouldMigrateInMysqlMode() throws Exception {
    String url = "jdbc:h2:mem:identity_migration_" + System.nanoTime()
        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      for (String table : EXPECTED_TABLES) {
        assertThat(tableExists(connection, table)).as("table %s exists", table).isTrue();
      }
    }
  }

  private boolean tableExists(Connection connection, String tableName) throws SQLException {
    try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[] {"TABLE"})) {
      return rs.next();
    }
  }
}
