package com.aituan.tradefulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class TradeFulfillmentSeedMigrationTest {
  @Test
  void emptyDatabaseCreatesARepeatableCrossServiceDemoOrder() throws Exception {
    String url = "jdbc:h2:mem:trade_seed_" + System.nanoTime()
        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    Flyway flyway = Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration", "classpath:db/seed")
        .load();

    flyway.migrate();
    flyway.migrate();

    try (var connection = DriverManager.getConnection(url, "sa", "");
         var statement = connection.prepareStatement(
             "select user_id, store_id, merchant_id, payable_amount from order_main where id = 9001")) {
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getLong("user_id")).isEqualTo(5001);
        assertThat(result.getLong("store_id")).isEqualTo(1);
        assertThat(result.getLong("merchant_id")).isEqualTo(1);
        assertThat(result.getBigDecimal("payable_amount")).isEqualByComparingTo("44.80");
      }
    }
  }
}
