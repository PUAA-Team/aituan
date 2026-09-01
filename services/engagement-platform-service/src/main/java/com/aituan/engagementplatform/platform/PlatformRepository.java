package com.aituan.engagementplatform.platform;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PlatformRepository {
  private final JdbcTemplate jdbc;
  PlatformRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  long countAnnouncements(String status) {
    Long value = status == null ? jdbc.queryForObject("select count(*) from platform_announcement where is_deleted=0", Long.class)
        : jdbc.queryForObject("select count(*) from platform_announcement where status=? and is_deleted=0", Long.class, status);
    return value == null ? 0 : value;
  }
  List<AnnouncementView> announcements(String status, int offset, int limit) {
    String q = "select id,title,content,target_client,cover_url,status,start_at,end_at,sort_order,created_by,updated_at from platform_announcement";
    return status == null ? jdbc.query(q + " where is_deleted=0 order by sort_order,id desc limit ? offset ?", this::mapAnnouncement, limit, offset)
        : jdbc.query(q + " where status=? and is_deleted=0 order by sort_order,id desc limit ? offset ?", this::mapAnnouncement, status, limit, offset);
  }
  Optional<AnnouncementView> announcement(long id) {
    return jdbc.query("select id,title,content,target_client,cover_url,status,start_at,end_at,sort_order,created_by,updated_at from platform_announcement where id=? and is_deleted=0", this::mapAnnouncement, id).stream().findFirst();
  }
  long insertAnnouncement(AnnouncementUpsertRequest r, long actorId) {
    jdbc.update("insert into platform_announcement(title,content,target_client,cover_url,status,start_at,end_at,sort_order,created_by) values(?,?,?,?,?,?,?,?,?)",
        r.title().trim(), r.content().trim(), text(r.targetClient(),"all"), r.coverUrl(), text(r.status(),"draft"), r.startAt(), r.endAt(), r.sortOrder()==null?0:r.sortOrder(), actorId);
    return jdbc.queryForObject("select max(id) from platform_announcement", Long.class);
  }
  void updateAnnouncement(long id, AnnouncementUpsertRequest r) {
    jdbc.update("update platform_announcement set title=?,content=?,target_client=?,cover_url=?,status=?,start_at=?,end_at=?,sort_order=?,updated_at=current_timestamp where id=? and is_deleted=0",
        r.title().trim(), r.content().trim(), text(r.targetClient(),"all"), r.coverUrl(), text(r.status(),"draft"), r.startAt(), r.endAt(), r.sortOrder()==null?0:r.sortOrder(), id);
  }
  void updateAnnouncementStatus(long id, String status) { jdbc.update("update platform_announcement set status=?,updated_at=current_timestamp where id=? and is_deleted=0", status, id); }

  List<ConfigView> configs() { return jdbc.query("select config_key,config_value,remark,updated_at from sys_config where is_deleted=0 order by config_key", this::mapConfig); }
  Optional<ConfigView> config(String key) { return jdbc.query("select config_key,config_value,remark,updated_at from sys_config where config_key=? and is_deleted=0", this::mapConfig, key).stream().findFirst(); }
  void upsertConfig(String key, String value, String remark) {
    int n=jdbc.update("update sys_config set config_value=?,remark=?,updated_at=current_timestamp,is_deleted=0 where config_key=?",value,remark,key);
    if(n==0) jdbc.update("insert into sys_config(config_key,config_value,remark) values(?,?,?)",key,value,remark);
  }

  long countAuditLogs(String action) {
    Long value=action==null?jdbc.queryForObject("select count(*) from sys_audit_log",Long.class):jdbc.queryForObject("select count(*) from sys_audit_log where action_type=?",Long.class,action);
    return value==null?0:value;
  }
  List<AuditLogView> auditLogs(String action,int offset,int limit) {
    String q="select id,actor_type,actor_id,action_type,target_type,target_id,detail,caller_service,created_at from sys_audit_log";
    return action==null?jdbc.query(q+" order by id desc limit ? offset ?",this::audit,limit,offset):jdbc.query(q+" where action_type=? order by id desc limit ? offset ?",this::audit,action,limit,offset);
  }
  long insertAudit(String caller,String key,InternalAuditLogRequest r) {
    jdbc.update("insert into sys_audit_log(actor_type,actor_id,action_type,target_type,target_id,detail,caller_service,idempotency_key) values(?,?,?,?,?,?,?,?)",r.actorType(),r.actorId(),r.actionType(),r.targetType(),r.targetId(),r.detail(),caller,key);
    return findAudit(caller,key).orElseThrow();
  }
  Optional<Long> findAudit(String caller,String key) { return jdbc.queryForList("select id from sys_audit_log where caller_service=? and idempotency_key=?",Long.class,caller,key).stream().findFirst(); }

  Map<String,Long> governanceMetrics() {
    Map<String,Long> m=new LinkedHashMap<>();
    m.put("reviews",count("review_record","is_deleted=0"));
    m.put("pendingReviews",count("review_record","replied=0 and status='published' and is_deleted=0"));
    m.put("reportedReviews",count("review_record","reported_count>0 and is_deleted=0"));
    m.put("pendingComplaints",count("complaint_ticket","status in ('pending','processing') and is_deleted=0"));
    m.put("openSessions",count("support_session","status='open' and is_deleted=0"));
    return m;
  }
  ReviewSummaryView reviewSummary(long storeId) {
    Map<String,Object> a=jdbc.queryForMap("select count(*) review_count,coalesce(avg(rating),0) average_rating from review_record where store_id=? and status='published' and is_deleted=0",storeId);
    Number average=(Number)a.get("average_rating");
    BigDecimal rating=BigDecimal.valueOf(average.doubleValue()).setScale(1,RoundingMode.HALF_UP);
    return new ReviewSummaryView(rating,((Number)a.get("review_count")).longValue(),reviewHighlights(storeId));
  }
  StoreEngagementView storeEngagement(long storeId) { ReviewSummaryView summary=reviewSummary(storeId);return new StoreEngagementView(summary.rating(),summary.count(),countWhere("select count(*) from review_record where store_id=? and replied=0 and status='published' and is_deleted=0",storeId),countWhere("select count(*) from support_session where store_id=? and status='open' and is_deleted=0",storeId)); }

  private List<String> reviewHighlights(long storeId){
    Map<String,Long> counts=new LinkedHashMap<>();
    for(String labels:jdbc.queryForList("select labels from review_record where store_id=? and status='published' and is_deleted=0 and labels is not null order by id desc limit 100",String.class,storeId)){
      if(labels==null)continue;
      for(String label:labels.split(",")){String value=label.trim();if(!value.isEmpty())counts.merge(value,1L,Long::sum);}
    }
    return counts.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).limit(5).map(Map.Entry::getKey).toList();
  }

  private long count(String table,String where){Long v=jdbc.queryForObject("select count(*) from "+table+" where "+where,Long.class);return v==null?0:v;}
  private long countWhere(String sql,long id){Long v=jdbc.queryForObject(sql,Long.class,id);return v==null?0:v;}
  private AnnouncementView mapAnnouncement(ResultSet r,int n)throws SQLException{return new AnnouncementView(r.getLong("id"),r.getString("title"),r.getString("content"),r.getString("target_client"),r.getString("cover_url"),r.getString("status"),time(r,"start_at"),time(r,"end_at"),r.getInt("sort_order"),r.getObject("created_by",Long.class),time(r,"updated_at"));}
  private ConfigView mapConfig(ResultSet r,int n)throws SQLException{return new ConfigView(r.getString("config_key"),r.getString("config_value"),r.getString("remark"),time(r,"updated_at"));}
  private AuditLogView audit(ResultSet r,int n)throws SQLException{return new AuditLogView(r.getLong("id"),r.getString("actor_type"),r.getObject("actor_id",Long.class),r.getString("action_type"),r.getString("target_type"),r.getObject("target_id",Long.class),r.getString("detail"),r.getString("caller_service"),time(r,"created_at"));}
  private LocalDateTime time(ResultSet r,String name)throws SQLException{Timestamp t=r.getTimestamp(name);return t==null?null:t.toLocalDateTime();}
  private String text(String v,String d){return v==null||v.isBlank()?d:v.trim();}
}
