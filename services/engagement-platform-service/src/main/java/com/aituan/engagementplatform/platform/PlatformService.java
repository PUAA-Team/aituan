package com.aituan.engagementplatform.platform;

import com.aituan.common.api.PageResponse;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.engagementplatform.client.PlatformRemoteClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PlatformService {
  private static final List<String> ANNOUNCEMENT_STATUS=List.of("draft","published","offline");
  private final PlatformRepository repository;
  private final PlatformRemoteClient remote;
  private final ObjectMapper mapper;
  PlatformService(PlatformRepository repository,PlatformRemoteClient remote,ObjectMapper mapper){this.repository=repository;this.remote=remote;this.mapper=mapper;}

  PageResponse<AnnouncementView> announcements(String status,int page,int size){String s=announcementStatus(status,true);return PageResponse.of(repository.announcements(s,(page-1)*size,size),page,size,repository.countAnnouncements(s));}
  @Transactional AnnouncementView create(AnnouncementUpsertRequest r){long id=repository.insertAnnouncement(r,CurrentUserContext.required().accountId());return announcement(id);}
  @Transactional AnnouncementView update(long id,AnnouncementUpsertRequest r){announcement(id);repository.updateAnnouncement(id,r);return announcement(id);}
  @Transactional AnnouncementView status(long id,StatusRequest r){announcement(id);repository.updateAnnouncementStatus(id,announcementStatus(r.status(),false));return announcement(id);}
  List<ConfigView> configs(){return repository.configs();}
  @Transactional List<ConfigView> updateConfig(String key,ConfigUpdateRequest r){repository.upsertConfig(key,r.configValue(),r.remark());return configs();}
  PageResponse<AuditLogView> audits(String action,int page,int size){String a=blank(action);return PageResponse.of(repository.auditLogs(a,(page-1)*size,size),page,size,repository.countAuditLogs(a));}
  DeliverySettingView deliverySettings(){return new DeliverySettingView(boolConfig("delivery.auto_advance",true),intConfig("delivery.tick_minutes",3));}
  @Transactional DeliverySettingView updateDelivery(DeliverySettingRequest r){boolean enabled=r.autoAdvanceEnabled()==null||r.autoAdvanceEnabled();int tick=r.tickMinutes()==null?3:r.tickMinutes();if(tick<1||tick>60)throw new BusinessException(ErrorCode.BAD_REQUEST,"推进间隔必须在 1 到 60 分钟");repository.upsertConfig("delivery.auto_advance",String.valueOf(enabled),"模拟配送自动推进");repository.upsertConfig("delivery.tick_minutes",String.valueOf(tick),"模拟配送推进间隔（分钟）");return deliverySettings();}

  DashboardView dashboard(){Map<String,Object> users=new LinkedHashMap<>(),merchants=new LinkedHashMap<>(),orders=new LinkedHashMap<>();boolean degraded=false;try{users=map(remote.identityMetrics());}catch(BusinessException e){degraded=true;}try{merchants=map(remote.merchantMetrics());}catch(BusinessException e){degraded=true;}try{orders=map(remote.orderMetrics());}catch(BusinessException e){degraded=true;}return new DashboardView(users,merchants,orders,repository.governanceMetrics(),degraded);}
  Map<String,Long> governance(){return repository.governanceMetrics();}
  ReviewSummaryView reviewSummary(long storeId){return repository.reviewSummary(storeId);}
  StoreEngagementView storeEngagement(long storeId){return repository.storeEngagement(storeId);}

  @Transactional InternalAuditLogView audit(String caller,String key,InternalAuditLogRequest r){if(key==null||key.isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"Idempotency-Key 不能为空");var existing=repository.findAudit(caller,key);if(existing.isPresent())return new InternalAuditLogView(existing.get(),true);try{return new InternalAuditLogView(repository.insertAudit(caller,key,r),false);}catch(DuplicateKeyException e){return new InternalAuditLogView(repository.findAudit(caller,key).orElseThrow(),true);}}

  private AnnouncementView announcement(long id){return repository.announcement(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));}
  private String announcementStatus(String value,boolean nullable){String v=blank(value);if(v==null&&nullable)return null;if(v==null||!ANNOUNCEMENT_STATUS.contains(v))throw new BusinessException(ErrorCode.BAD_REQUEST,"公告状态不正确");return v;}
  private String blank(String v){return v==null||v.isBlank()?null:v.trim().toLowerCase();}
  private boolean boolConfig(String key,boolean d){return repository.config(key).map(ConfigView::configValue).map(Boolean::parseBoolean).orElse(d);}
  private int intConfig(String key,int d){try{return repository.config(key).map(ConfigView::configValue).map(Integer::parseInt).orElse(d);}catch(NumberFormatException e){return d;}}
  private Map<String,Object> map(JsonNode node){return mapper.convertValue(node,new TypeReference<>(){});}
}
