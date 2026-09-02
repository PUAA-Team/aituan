package com.aituan.tradefulfillment;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TradeFulfillmentMigrationTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void createsTradeOwnedTables() {
    List<String> tables = List.of(
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

    for (String table : tables) {
      assertThatCode(() -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class))
          .as("table %s should be created by Flyway", table)
          .doesNotThrowAnyException();
    }

    assertThatCode(() -> jdbcTemplate.queryForList(
        "SELECT store_name_snapshot, business_type_snapshot FROM cart"))
        .as("cart should own a durable store snapshot for catalog-outage reads")
        .doesNotThrowAnyException();
    assertThatCode(() -> jdbcTemplate.queryForList(
        """
        SELECT item_name_snapshot, item_subtitle_snapshot, category_name_snapshot,
               unit_price_snapshot, stock_snapshot, status_snapshot
        FROM cart_item
        """))
        .as("cart items should own durable product snapshots for catalog-outage reads")
        .doesNotThrowAnyException();
  }
}
