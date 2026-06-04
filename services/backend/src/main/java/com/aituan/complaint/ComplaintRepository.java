package com.aituan.complaint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ComplaintRepository {
  private final JdbcTemplate jdbcTemplate;

  ComplaintRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String TICKET_SELECT = """
      select t.id, t.ticket_no, t.user_id, t.order_id, t.store_id, t.merchant_id,
             t.category, t.title, t.detail, t.evidence_urls, t.status,
             t.accepted_by, t.accepted_at, t.resolved_by, t.resolved_at, t.closed_at,
             t.created_at, t.updated_at,
             o.order_no as order_no,
             ms.store_name as store_name,
             up.nickname as user_nickname
      from complaint_ticket t
      left join order_main o on o.id = t.order_id and o.is_deleted = 0
      left join merchant_store ms on ms.id = t.store_id and ms.is_deleted = 0
      left join user_profile up on up.id = t.user_id and up.is_deleted = 0
      """;

  Optional<TicketRow> findById(long id) {
    List<TicketRow> rows = jdbcTemplate.query(
        TICKET_SELECT + " where t.id = ? and t.is_deleted = 0 limit 1",
        this::mapTicket, id);
    return rows.stream().findFirst();
  }

  // ============ 用户端 ============

  long countUserTickets(long userId, String statusFilter) {
    if (statusFilter == null) {
      Long c = jdbcTemplate.queryForObject(
          "select count(1) from complaint_ticket where user_id = ? and is_deleted = 0",
          Long.class, userId);
      return c == null ? 0 : c;
    }
    Long c = jdbcTemplate.queryForObject(
        "select count(1) from complaint_ticket where user_id = ? and status = ? and is_deleted = 0",
        Long.class, userId, statusFilter);
    return c == null ? 0 : c;
  }

  List<TicketRow> listUserTickets(long userId, String statusFilter, int offset, int limit) {
    String tail = " order by t.created_at desc, t.id desc limit ? offset ?";
    if (statusFilter == null) {
      return jdbcTemplate.query(
          TICKET_SELECT + " where t.user_id = ? and t.is_deleted = 0" + tail,
          this::mapTicket, userId, limit, offset);
    }
    return jdbcTemplate.query(
        TICKET_SELECT + " where t.user_id = ? and t.status = ? and t.is_deleted = 0" + tail,
        this::mapTicket, userId, statusFilter, limit, offset);
  }

  // ============ 后台端 ============

  long countAdminTickets(String statusFilter, String categoryFilter, String orderNoFilter, String storeNameFilter) {
    StringBuilder sql = new StringBuilder("""
        select count(1)
        from complaint_ticket t
        left join order_main o on o.id = t.order_id and o.is_deleted = 0
        left join merchant_store ms on ms.id = t.store_id and ms.is_deleted = 0
        where t.is_deleted = 0
        """);
    List<Object> args = new ArrayList<>();
    appendAdminFilters(sql, args, statusFilter, categoryFilter, orderNoFilter, storeNameFilter);
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return c == null ? 0 : c;
  }

  List<TicketRow> listAdminTickets(
      String statusFilter, String categoryFilter, String orderNoFilter, String storeNameFilter, int offset, int limit) {
    StringBuilder sql = new StringBuilder(TICKET_SELECT).append(" where t.is_deleted = 0");
    List<Object> args = new ArrayList<>();
    appendAdminFilters(sql, args, statusFilter, categoryFilter, orderNoFilter, storeNameFilter);
    sql.append(" order by t.created_at desc, t.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapTicket, args.toArray());
  }

  // ============ 商家端 ============

  long countMerchantTickets(long merchantId, String statusFilter, String orderNoFilter, String storeNameFilter) {
    StringBuilder sql = new StringBuilder("""
        select count(1)
        from complaint_ticket t
        left join order_main o on o.id = t.order_id and o.is_deleted = 0
        left join merchant_store ms on ms.id = t.store_id and ms.is_deleted = 0
        where t.merchant_id = ? and t.is_deleted = 0
        """);
    List<Object> args = new ArrayList<>();
    args.add(merchantId);
    appendMerchantFilters(sql, args, statusFilter, orderNoFilter, storeNameFilter);
    Long c = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return c == null ? 0 : c;
  }

  List<TicketRow> listMerchantTickets(
      long merchantId, String statusFilter, String orderNoFilter, String storeNameFilter, int offset, int limit) {
    StringBuilder sql = new StringBuilder(TICKET_SELECT).append(" where t.merchant_id = ? and t.is_deleted = 0");
    List<Object> args = new ArrayList<>();
    args.add(merchantId);
    appendMerchantFilters(sql, args, statusFilter, orderNoFilter, storeNameFilter);
    sql.append(" order by t.created_at desc, t.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapTicket, args.toArray());
  }

  private void appendAdminFilters(
      StringBuilder sql, List<Object> args,
      String statusFilter, String categoryFilter, String orderNoFilter, String storeNameFilter) {
    if (statusFilter != null) { sql.append(" and t.status = ?"); args.add(statusFilter); }
    if (categoryFilter != null) { sql.append(" and t.category = ?"); args.add(categoryFilter); }
    if (orderNoFilter != null) { sql.append(" and o.order_no = ?"); args.add(orderNoFilter); }
    if (storeNameFilter != null) { sql.append(" and ms.store_name like ?"); args.add("%" + storeNameFilter + "%"); }
  }

  private void appendMerchantFilters(
      StringBuilder sql, List<Object> args,
      String statusFilter, String orderNoFilter, String storeNameFilter) {
    if (statusFilter != null) { sql.append(" and t.status = ?"); args.add(statusFilter); }
    if (orderNoFilter != null) { sql.append(" and o.order_no = ?"); args.add(orderNoFilter); }
    if (storeNameFilter != null) { sql.append(" and ms.store_name like ?"); args.add("%" + storeNameFilter + "%"); }
  }

  // ============ 写入 ============

  Long insertTicket(String ticketNo, long userId, Long orderId, Long storeId, Long merchantId,
                    String category, String title, String detail, String evidenceUrls) {
    jdbcTemplate.update(
        """
        insert into complaint_ticket(ticket_no, user_id, order_id, store_id, merchant_id,
                                     category, title, detail, evidence_urls, status)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
        """,
        ticketNo, userId, orderId, storeId, merchantId, category, title, detail, evidenceUrls);
    return jdbcTemplate.queryForObject(
        "select id from complaint_ticket where ticket_no = ?", Long.class, ticketNo);
  }

  void accept(long id, long adminAccountId) {
    jdbcTemplate.update(
        "update complaint_ticket set status = 'processing', accepted_by = ?, accepted_at = current_timestamp, updated_at = current_timestamp where id = ?",
        adminAccountId, id);
  }

  void resolve(long id, long adminAccountId) {
    jdbcTemplate.update(
        "update complaint_ticket set status = 'resolved', resolved_by = ?, resolved_at = current_timestamp, updated_at = current_timestamp where id = ?",
        adminAccountId, id);
  }

  void close(long id) {
    jdbcTemplate.update(
        "update complaint_ticket set status = 'closed', closed_at = current_timestamp, updated_at = current_timestamp where id = ?",
        id);
  }

  Long insertLog(long ticketId, String action, String operatorType, Long operatorId, String remark) {
    jdbcTemplate.update(
        "insert into complaint_log(ticket_id, action, operator_type, operator_id, remark) values (?, ?, ?, ?, ?)",
        ticketId, action, operatorType, operatorId, remark);
    return jdbcTemplate.queryForObject(
        "select max(id) from complaint_log where ticket_id = ?", Long.class, ticketId);
  }

  List<LogRow> listLogs(long ticketId) {
    return jdbcTemplate.query(
        "select id, ticket_id, action, operator_type, operator_id, remark, created_at "
            + "from complaint_log where ticket_id = ? order by id asc",
        this::mapLog, ticketId);
  }

  // ============ 关联查询 ============

  Optional<OrderRefRow> findOrderForComplaint(long userId, long orderId) {
    List<OrderRefRow> rows = jdbcTemplate.query(
        "select id, order_no, store_id from order_main where id = ? and user_id = ? and is_deleted = 0 limit 1",
        (rs, n) -> new OrderRefRow(rs.getLong("id"), rs.getString("order_no"), rs.getLong("store_id")),
        orderId, userId);
    return rows.stream().findFirst();
  }

  Optional<Long> findStoreMerchantId(long storeId) {
    List<Long> rows = jdbcTemplate.queryForList(
        "select merchant_id from merchant_store where id = ? and is_deleted = 0", Long.class, storeId);
    return rows.stream().findFirst();
  }

  Optional<Long> findMerchantIdByAccount(long accountId) {
    List<Long> rows = jdbcTemplate.queryForList(
        "select id from merchant_profile where account_id = ? and is_deleted = 0 limit 1",
        Long.class, accountId);
    return rows.stream().findFirst();
  }

  void insertSysAuditLog(String actorType, long actorId, String actionType, String targetType, long targetId, String detail) {
    jdbcTemplate.update(
        "insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values (?, ?, ?, ?, ?, ?)",
        actorType, actorId, actionType, targetType, targetId, detail);
  }

  // ============ 行映射 ============

  private TicketRow mapTicket(ResultSet rs, int rowNum) throws SQLException {
    long orderIdRaw = rs.getLong("order_id");
    Long orderId = rs.wasNull() ? null : orderIdRaw;
    long storeIdRaw = rs.getLong("store_id");
    Long storeId = rs.wasNull() ? null : storeIdRaw;
    long acceptedByRaw = rs.getLong("accepted_by");
    Long acceptedBy = rs.wasNull() ? null : acceptedByRaw;
    long resolvedByRaw = rs.getLong("resolved_by");
    Long resolvedBy = rs.wasNull() ? null : resolvedByRaw;
    Timestamp acceptedAt = rs.getTimestamp("accepted_at");
    Timestamp resolvedAt = rs.getTimestamp("resolved_at");
    Timestamp closedAt = rs.getTimestamp("closed_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new TicketRow(
        rs.getLong("id"), rs.getString("ticket_no"), rs.getLong("user_id"),
        orderId, rs.getString("order_no"),
        storeId, rs.getString("store_name"), rs.getObject("merchant_id", Long.class),
        rs.getString("category"), rs.getString("title"), rs.getString("detail"),
        rs.getString("evidence_urls"), rs.getString("status"),
        acceptedBy, acceptedAt == null ? null : acceptedAt.toLocalDateTime(),
        resolvedBy, resolvedAt == null ? null : resolvedAt.toLocalDateTime(),
        closedAt == null ? null : closedAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        rs.getString("user_nickname"));
  }

  private LogRow mapLog(ResultSet rs, int rowNum) throws SQLException {
    long operatorIdRaw = rs.getLong("operator_id");
    Long operatorId = rs.wasNull() ? null : operatorIdRaw;
    Timestamp t = rs.getTimestamp("created_at");
    return new LogRow(
        rs.getLong("id"), rs.getLong("ticket_id"), rs.getString("action"),
        rs.getString("operator_type"), operatorId, rs.getString("remark"),
        t == null ? null : t.toLocalDateTime());
  }

  record TicketRow(Long id, String ticketNo, Long userId,
                   Long orderId, String orderNo,
                   Long storeId, String storeName, Long merchantId,
                   String category, String title, String detail,
                   String evidenceUrls, String status,
                   Long acceptedBy, LocalDateTime acceptedAt,
                   Long resolvedBy, LocalDateTime resolvedAt,
                   LocalDateTime closedAt, LocalDateTime createdAt,
                   String userNickname) {}

  record LogRow(Long id, Long ticketId, String action, String operatorType,
                Long operatorId, String remark, LocalDateTime createdAt) {}

  record OrderRefRow(Long id, String orderNo, Long storeId) {}
}
