package com.aituan.merchantcatalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class MerchantCatalogFlywayMigrationSmokeTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:aituan_merchant_smoke;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

  private static final List<String> EXPECTED_TABLES = List.of(
      "merchant_profile",
      "merchant_store",
      "merchant_delivery_rule",
      "merchant_takeaway_setting",
      "merchant_application",
      "merchant_certification_material",
      "merchant_audit_log",
      "catalog_category",
      "catalog_item",
      "catalog_sku",
      "catalog_item_tag",
      "catalog_item_tag_rel",
      "ops_banner_config",
      "member_recommend_config",
      "inventory_idempotency_record");

  @Test
  void emptyDatabaseMigrationCreatesAllMerchantTablesAndIsRepeatable() throws Exception {
    Flyway flyway = Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/merchant", "classpath:db/seed/merchant")
        .load();

    flyway.migrate();

    List<String> tables = listTables();
    assertThat(tables).containsAll(EXPECTED_TABLES);

    // 重复执行迁移和 seed 不应报错（幂等）。
    flyway.migrate();
    assertThat(listTables()).containsAll(EXPECTED_TABLES);
  }

  private List<String> listTables() throws Exception {
    List<String> tables = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery(
             "select table_name from information_schema.tables where table_type = 'BASE TABLE'")) {
      while (rs.next()) {
        tables.add(rs.getString("table_name").toLowerCase());
      }
    }
    return tables;
  }
}
