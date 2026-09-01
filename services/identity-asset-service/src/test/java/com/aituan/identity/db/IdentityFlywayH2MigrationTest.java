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
      "user_profile",
      "user_address",
      "user_favorite",
      "support_station_message",
      "member_level",
      "coupon_template",
      "user_coupon",
      "member_growth_log");

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
