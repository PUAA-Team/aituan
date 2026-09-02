package com.aituan.identity.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class IdentityFlywayMysqlSmokeTest {
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
  @EnabledIfEnvironmentVariable(named = "AITUAN_IDENTITY_MYSQL_SMOKE", matches = "true")
  void emptyMysqlDatabaseShouldMigrate() throws Exception {
    String url = requiredEnv("AITUAN_IDENTITY_SMOKE_JDBC_URL");
    String username = requiredEnv("AITUAN_IDENTITY_SMOKE_DB_USERNAME");
    String password = requiredEnv("AITUAN_IDENTITY_SMOKE_DB_PASSWORD");

    Flyway.configure()
        .dataSource(url, username, password)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()
        .clean();

    Flyway.configure()
        .dataSource(url, username, password)
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, username, password)) {
      for (String table : EXPECTED_TABLES) {
        assertThat(tableExists(connection, table)).as("table %s exists", table).isTrue();
      }
    }
  }

  private String requiredEnv(String name) {
    String value = System.getenv(name);
    assertThat(value).as(name).isNotBlank();
    return value;
  }

  private boolean tableExists(Connection connection, String tableName) throws SQLException {
    try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, new String[] {"TABLE"})) {
      return rs.next();
    }
  }
}
