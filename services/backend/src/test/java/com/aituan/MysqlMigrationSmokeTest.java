package com.aituan;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 使用真实 MySQL profile 时，验证 Flyway 迁移和基础种子数据可正常落库。 */
@SpringBootTest
@ActiveProfiles("mysql-ci")
@EnabledIfEnvironmentVariable(named = "AITUAN_MYSQL_CI_ENABLED", matches = "true")
class MysqlMigrationSmokeTest {
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void mysqlProfileShouldRunMigrationsAndSeedDemoAccounts() throws Exception {
    assertThat(dataSource.getConnection().getMetaData().getDatabaseProductName()).contains("MySQL");

    Integer migrationCount = jdbcTemplate.queryForObject(
        "select count(*) from flyway_schema_history where success = 1",
        Integer.class);
    Integer accountCount = jdbcTemplate.queryForObject(
        "select count(*) from auth_account where account_name in ('demo_user', 'demo_merchant', 'demo_admin')",
        Integer.class);

    assertThat(migrationCount).isNotNull().isGreaterThan(0);
    assertThat(accountCount).isEqualTo(3);
  }
}
