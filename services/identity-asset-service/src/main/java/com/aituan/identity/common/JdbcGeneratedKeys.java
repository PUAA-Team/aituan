package com.aituan.identity.common;

import java.sql.PreparedStatement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

public final class JdbcGeneratedKeys {
  private JdbcGeneratedKeys() {}

  public static long insertAndReturnId(JdbcTemplate jdbcTemplate, String sql, Object... params) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      return statement;
    }, keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("未能获取数据库自增主键");
    }
    return key.longValue();
  }
}
