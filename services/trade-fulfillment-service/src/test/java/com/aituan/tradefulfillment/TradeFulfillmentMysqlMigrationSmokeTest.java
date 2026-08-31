package com.aituan.tradefulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class TradeFulfillmentMysqlMigrationSmokeTest {

  @Test
  @EnabledIfEnvironmentVariable(named = "AITUAN_TRADE_MYSQL_CI_ENABLED", matches = "true")
  void migratesTradeSchemaOnMysql8() throws Exception {
    String url = requiredEnv("TRADE_DB_URL");
    String username = requiredEnv("TRADE_DB_USERNAME");
    String password = requiredEnv("TRADE_DB_PASSWORD");

    Flyway.configure()
        .dataSource(url, username, password)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, username, password)) {
      for (String table : tradeTables()) {
        assertThat(tableExists(connection, table))
            .as("MySQL should contain trade-owned table %s", table)
            .isTrue();
      }
    }
  }

  private boolean tableExists(Connection connection, String tableName) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
        "select count(*) from information_schema.tables where table_schema = database() and table_name = ?")) {
      statement.setString(1, tableName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1) > 0;
      }
    }
  }

  private List<String> tradeTables() {
    return List.of(
        "cart",
        "cart_item",
        "order_main",
        "order_item",
        "order_payment_record",
        "order_voucher",
        "order_booking_record",
        "order_refund_record",
        "order_state_log",
        "delivery_task",
        "delivery_track_node");
  }

  private String requiredEnv(String name) {
    String value = System.getenv(name);
    assertThat(value).as("%s must be set when MySQL smoke test is enabled", name).isNotBlank();
    return value;
  }
}
