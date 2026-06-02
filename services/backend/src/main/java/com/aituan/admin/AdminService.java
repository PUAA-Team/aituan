package com.aituan.admin;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.file.FileAssetView;
import com.aituan.common.file.FileStorageService;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class AdminService {
  private static final int DELIVERY_TICK_MINUTES = 3;
  private static final String DEFAULT_MERCHANT_PASSWORD = "123456";

  private final AdminRepository adminRepository;
  private final FileStorageService fileStorageService;
  private final PasswordEncoder passwordEncoder;

  AdminService(AdminRepository adminRepository, FileStorageService fileStorageService, PasswordEncoder passwordEncoder) {
    this.adminRepository = adminRepository;
    this.fileStorageService = fileStorageService;
    this.passwordEncoder = passwordEncoder;
  }

  AdminDashboardView dashboard() {
    requireAdmin();
    return new AdminDashboardView(
        adminRepository.count("select count(1) from order_main where cast(created_at as date) = current_date and is_deleted = 0"),
        adminRepository.sum("select coalesce(sum(payable_amount), 0) from order_main where cast(created_at as date) = current_date and payment_status = 'paid' and is_deleted = 0"),
        adminRepository.count("select count(1) from order_main where fulfillment_status = 'abnormal' and is_deleted = 0"),
        adminRepository.count("select count(1) from merchant_profile where is_deleted = 0"),
        adminRepository.count("select count(1) from user_profile where is_deleted = 0"),
        adminRepository.count("select count(1) from catalog_item where is_deleted = 0 and status = 'on_sale'"),
        adminRepository.count("select count(1) from delivery_task where current_stage in ('ready_for_delivery', 'delivering') and is_deleted = 0"));
  }

  AdminProfileView adminProfile() {
    CurrentUser current = requireAdmin();
    return adminRepository.findAdminProfile(current.accountId())
        .map(this::toAdminProfileView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminMerchantView> merchants(String keyword, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countMerchants(keyword);
    List<AdminMerchantView> list = adminRepository.listMerchants(keyword, (page - 1) * pageSize, pageSize).stream().map(this::toMerchantView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  PageResponse<AdminMerchantApplicationView> merchantApplications(String status, int page, int pageSize) {
    requireAdmin();
    String normalizedStatus = normalizeNullableAuditStatus(status);
    long total = adminRepository.countApplications(normalizedStatus);
    List<AdminMerchantApplicationView> list = adminRepository.listApplications(normalizedStatus, (page - 1) * pageSize, pageSize).stream().map(this::toApplicationView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminMerchantApplicationView approveMerchantApplication(long id, AdminMerchantApplicationAuditRequest request) {
    CurrentUser current = requireAdmin();
    AdminRepository.ApplicationRow application = pendingApplication(id);
    String remark = auditRemark(request, "审核通过，商家可用申请编号登录");
    long accountId = adminRepository.insertMerchantAccount("M" + System.currentTimeMillis(), application.applicationNo(), passwordEncoder.encode(DEFAULT_MERCHANT_PASSWORD));
    adminRepository.insertAccountRole(accountId, 2L);
    long merchantId = adminRepository.insertMerchantFromApplication("MCH" + System.currentTimeMillis(), accountId, application);
    adminRepository.insertStoreFromApplication(merchantId, application);
    adminRepository.updateApplicationAudit(id, "approved", accountId, current.accountId(), remark);
    adminRepository.insertMerchantAuditLog("application", id, "approve", "approved", remark, current.accountId());
    adminRepository.insertSysAuditLog(current.accountId(), "merchant_application_approve", "merchant_application", id, application.applicationNo() + " 入驻申请已通过");
    return adminRepository.findApplication(id).map(this::toApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminMerchantApplicationView rejectMerchantApplication(long id, AdminMerchantApplicationAuditRequest request) {
    CurrentUser current = requireAdmin();
    AdminRepository.ApplicationRow application = pendingApplication(id);
    String remark = auditRemark(request, "资料不完整，请补充后重新提交");
    adminRepository.updateApplicationAudit(id, "rejected", application.accountId(), current.accountId(), remark);
    adminRepository.insertMerchantAuditLog("application", id, "reject", "rejected", remark, current.accountId());
    adminRepository.insertSysAuditLog(current.accountId(), "merchant_application_reject", "merchant_application", id, application.applicationNo() + " 入驻申请已驳回");
    return adminRepository.findApplication(id).map(this::toApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminCertificationMaterialView> certificationMaterials(String status, int page, int pageSize) {
    requireAdmin();
    String normalizedStatus = normalizeNullableAuditStatus(status);
    long total = adminRepository.countCertificationMaterials(normalizedStatus);
    List<AdminCertificationMaterialView> list = adminRepository.listCertificationMaterials(normalizedStatus, (page - 1) * pageSize, pageSize).stream().map(this::toMaterialView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminCertificationMaterialView updateCertificationMaterialStatus(long id, AdminCertificationMaterialAuditRequest request) {
    CurrentUser current = requireAdmin();
    String status = normalizeAuditStatus(request.status());
    String remark = request.rejectReason() == null || request.rejectReason().isBlank() ? ("approved".equals(status) ? "材料审核通过" : "材料审核未通过") : request.rejectReason().trim();
    adminRepository.updateCertificationMaterialStatus(id, status, remark, current.accountId());
    adminRepository.insertMerchantAuditLog("certification_material", id, "audit", status, remark, current.accountId());
    adminRepository.insertSysAuditLog(current.accountId(), "merchant_material_audit", "certification_material", id, remark);
    return adminRepository.findCertificationMaterial(id).map(this::toMaterialView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminStoreView> stores(Long merchantId, String businessType, String status, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countStores(merchantId, businessType, status);
    List<AdminStoreView> list = adminRepository.listStores(merchantId, businessType, status, (page - 1) * pageSize, pageSize).stream().map(this::toStoreView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminMerchantView createMerchant(AdminMerchantUpsertRequest request) {
    requireAdmin();
    long id = adminRepository.insertMerchant("MCH" + System.currentTimeMillis(), request, normalizeGeneralStatus(defaultValue(request.status(), "normal")), normalizeAuditStatus(defaultValue(request.auditStatus(), "approved")));
    return merchantView(id);
  }

  @Transactional
  AdminMerchantView updateMerchant(long merchantId, AdminMerchantUpsertRequest request) {
    requireAdmin();
    adminRepository.updateMerchant(merchantId, request, normalizeGeneralStatus(defaultValue(request.status(), "normal")), normalizeAuditStatus(defaultValue(request.auditStatus(), "approved")));
    return merchantView(merchantId);
  }

  @Transactional
  AdminMerchantView updateMerchantStatus(long merchantId, AdminStatusRequest request) {
    requireAdmin();
    adminRepository.updateMerchantStatus(merchantId, normalizeGeneralStatus(request.status()));
    return adminRepository.listMerchants(null, 0, 200).stream()
        .filter(row -> row.merchantId().equals(merchantId))
        .findFirst()
        .map(this::toMerchantView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView createStore(AdminStoreUpsertRequest request) {
    requireAdmin();
    long id = adminRepository.insertStore(request, normalizeBusinessType(request.businessType()), normalizeStoreStatus(defaultValue(request.status(), "open")));
    return storeView(id);
  }

  @Transactional
  AdminStoreView updateStore(long storeId, AdminStoreUpsertRequest request) {
    requireAdmin();
    adminRepository.updateStore(storeId, request, normalizeBusinessType(request.businessType()), normalizeStoreStatus(defaultValue(request.status(), "open")));
    return storeView(storeId);
  }

  @Transactional
  AdminStoreView updateStoreStatus(long storeId, AdminStatusRequest request) {
    requireAdmin();
    adminRepository.updateStoreStatus(storeId, normalizeStoreStatus(request.status()));
    return adminRepository.listStores(null, null, null, 0, 500).stream()
        .filter(row -> row.storeId().equals(storeId))
        .findFirst()
        .map(this::toStoreView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView uploadStoreCover(long storeId, MultipartFile file) {
    requireAdmin();
    FileAssetView asset = fileStorageService.save(file, "store");
    adminRepository.updateStoreCover(storeId, asset.publicUrl());
    return adminRepository.listStores(null, null, null, 0, 500).stream()
        .filter(row -> row.storeId().equals(storeId))
        .findFirst()
        .map(this::toStoreView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminUserView> users(String keyword, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countUsers(keyword);
    List<AdminUserView> list = adminRepository.listUsers(keyword, (page - 1) * pageSize, pageSize).stream().map(this::toUserView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminUserView updateUser(long accountId, AdminUserUpdateRequest request) {
    requireAdmin();
    adminRepository.updateUser(accountId, request, normalizeGeneralStatus(defaultValue(request.status(), "normal")));
    return userView(accountId);
  }

  @Transactional
  AdminUserView updateUserStatus(long accountId, AdminStatusRequest request) {
    requireAdmin();
    adminRepository.updateUserStatus(accountId, normalizeGeneralStatus(request.status()));
    return adminRepository.listUsers(null, 0, 500).stream()
        .filter(row -> row.accountId().equals(accountId))
        .findFirst()
        .map(this::toUserView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminDeliveryTaskView> deliveryTasks(String stage, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countDeliveryTasks(stage);
    List<AdminDeliveryTaskView> list = adminRepository.listDeliveryTasks(stage, (page - 1) * pageSize, pageSize).stream().map(this::toDeliveryTaskView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  AdminDeliveryTaskView deliveryTask(long taskId) {
    requireAdmin();
    return adminRepository.findDeliveryTask(taskId).map(this::toDeliveryTaskView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminDeliveryTaskView advanceDeliveryTask(long taskId, AdminDeliveryActionRequest request) {
    CurrentUser current = requireAdmin();
    AdminRepository.DeliveryTaskRow task = adminRepository.findDeliveryTask(taskId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    NextDeliveryStage next = nextStage(task.currentStage());
    if (next == null) {
      return toDeliveryTaskView(task);
    }
    LocalDateTime nextTickAt = next.completed() ? null : LocalDateTime.now().plusMinutes(DELIVERY_TICK_MINUTES);
    adminRepository.updateDeliveryStage(task, next.stage(), next.text(), next.displayStatus(), next.completed(), nextTickAt, current.accountId(), request == null ? null : request.remark());
    return deliveryTask(taskId);
  }

  @Transactional
  AdminDeliveryTaskView pauseDeliveryTask(long taskId) {
    CurrentUser current = requireAdmin();
    adminRepository.pauseDeliveryTask(taskId, current.accountId());
    return deliveryTask(taskId);
  }

  @Transactional
  AdminDeliveryTaskView resumeDeliveryTask(long taskId) {
    CurrentUser current = requireAdmin();
    adminRepository.resumeDeliveryTask(taskId, current.accountId());
    return deliveryTask(taskId);
  }

  @Transactional
  AdminDeliveryTaskView markDeliveryAbnormal(long taskId, AdminDeliveryActionRequest request) {
    CurrentUser current = requireAdmin();
    adminRepository.markDeliveryAbnormal(taskId, request == null || request.reason() == null ? "后台标记异常" : request.reason(), current.accountId());
    return deliveryTask(taskId);
  }

  AdminDeliverySettingView deliverySettings() {
    requireAdmin();
    return new AdminDeliverySettingView(
        Boolean.parseBoolean(adminRepository.configValue("delivery_auto_advance_enabled", "true")),
        Integer.parseInt(adminRepository.configValue("delivery_tick_minutes", "3")));
  }

  @Transactional
  AdminDeliverySettingView updateDeliverySettings(AdminDeliverySettingRequest request) {
    requireAdmin();
    if (request.autoAdvanceEnabled() != null) {
      adminRepository.upsertConfig("delivery_auto_advance_enabled", String.valueOf(request.autoAdvanceEnabled()), "模拟配送自动推进开关");
    }
    if (request.tickMinutes() != null) {
      adminRepository.upsertConfig("delivery_tick_minutes", String.valueOf(Math.max(1, request.tickMinutes())), "模拟配送推进间隔分钟数");
    }
    return deliverySettings();
  }

  PageResponse<AdminAnnouncementView> announcements(String status, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countAnnouncements(status);
    List<AdminAnnouncementView> list = adminRepository.listAnnouncements(status, (page - 1) * pageSize, pageSize).stream().map(this::toAnnouncementView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminAnnouncementView createAnnouncement(AdminAnnouncementUpsertRequest request) {
    CurrentUser current = requireAdmin();
    long id = adminRepository.insertAnnouncement(request, current.accountId());
    return adminRepository.findAnnouncement(id).map(this::toAnnouncementView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminAnnouncementView updateAnnouncement(long id, AdminAnnouncementUpsertRequest request) {
    requireAdmin();
    adminRepository.updateAnnouncement(id, request);
    return adminRepository.findAnnouncement(id).map(this::toAnnouncementView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminAnnouncementView updateAnnouncementStatus(long id, AdminStatusRequest request) {
    requireAdmin();
    adminRepository.updateAnnouncementStatus(id, normalizeAnnouncementStatus(request.status()));
    return adminRepository.findAnnouncement(id).map(this::toAnnouncementView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  List<AdminConfigView> configs() {
    requireAdmin();
    return adminRepository.listConfigs().stream().map(this::toConfigView).toList();
  }

  @Transactional
  List<AdminConfigView> updateConfig(String key, AdminConfigUpdateRequest request) {
    requireAdmin();
    adminRepository.upsertConfig(key, request.configValue(), request.remark());
    return configs();
  }

  PageResponse<AdminAuditLogView> auditLogs(String actionType, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countAuditLogs(actionType);
    List<AdminAuditLogView> list = adminRepository.listAuditLogs(actionType, (page - 1) * pageSize, pageSize).stream().map(this::toAuditView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  private CurrentUser requireAdmin() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private NextDeliveryStage nextStage(String currentStage) {
    return switch (currentStage) {
      case "merchant_pending" -> new NextDeliveryStage("accepted", "商家已接单", "pending", false);
      case "accepted" -> new NextDeliveryStage("preparing", "商家正在备餐", "pending", false);
      case "preparing" -> new NextDeliveryStage("ready_for_delivery", "餐品已出餐，待配送", "pending", false);
      case "ready_for_delivery" -> new NextDeliveryStage("delivering", "骑手正在配送", "pending", false);
      case "delivering" -> new NextDeliveryStage("delivered", "订单已送达", "pending", false);
      case "delivered" -> new NextDeliveryStage("completed", "订单已完成", "used", true);
      case "completed" -> null;
      default -> throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    };
  }

  private String normalizeGeneralStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "normal", "disabled", "blocked" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值不正确");
    };
  }

  private String normalizeStoreStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "open", "closed" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "门店状态只能是 open 或 closed");
    };
  }

  private String normalizeAuditStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "pending", "approved", "rejected" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态不正确");
    };
  }

  private String normalizeNullableAuditStatus(String status) {
    return status == null || status.isBlank() ? null : normalizeAuditStatus(status);
  }

  private String normalizeBusinessType(String businessType) {
    String value = businessType == null ? "" : businessType.trim().toLowerCase();
    return switch (value) {
      case "takeaway", "group_buy", "hotel", "entertainment", "movie", "beauty", "ticket", "massage" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "业务类型不正确");
    };
  }

  private String defaultValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String auditRemark(AdminMerchantApplicationAuditRequest request, String fallback) {
    return request == null || request.remark() == null || request.remark().isBlank() ? fallback : request.remark().trim();
  }

  private AdminRepository.ApplicationRow pendingApplication(long id) {
    AdminRepository.ApplicationRow application = adminRepository.findApplication(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"pending".equals(application.status())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "申请已处理");
    }
    return application;
  }

  private AdminMerchantView merchantView(long merchantId) {
    return adminRepository.findMerchant(merchantId).map(this::toMerchantView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private AdminStoreView storeView(long storeId) {
    return adminRepository.findStore(storeId).map(this::toStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private AdminUserView userView(long accountId) {
    return adminRepository.findUser(accountId).map(this::toUserView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private String normalizeAnnouncementStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "draft", "published", "offline" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "公告状态不正确");
    };
  }

  private AdminProfileView toAdminProfileView(AdminRepository.AdminProfileRow row) {
    String nickname = row.nickname() == null || row.nickname().isBlank() ? row.accountNo() : row.nickname();
    return new AdminProfileView(row.accountId(), row.accountNo(), row.accountType(), nickname, row.phone(), row.email(), row.status(), row.createdAt(), row.lastLoginAt());
  }

  private AdminMerchantView toMerchantView(AdminRepository.MerchantRow row) {
    return new AdminMerchantView(row.merchantId(), row.accountId(), row.merchantName(), row.contactName(), row.contactPhone(), row.licenseNo(), row.status(), row.auditStatus(), row.storeCount(), row.itemCount(), row.settledAt());
  }

  private AdminMerchantApplicationView toApplicationView(AdminRepository.ApplicationRow row) {
    return new AdminMerchantApplicationView(row.id(), row.applicationNo(), row.accountId(), row.merchantName(), row.contactName(), row.contactPhone(), row.businessType(), row.storeName(), row.address(), row.status(), row.auditRemark(), row.submittedAt(), row.auditedBy(), row.auditedAt());
  }

  private AdminCertificationMaterialView toMaterialView(AdminRepository.CertificationMaterialRow row) {
    return new AdminCertificationMaterialView(row.id(), row.merchantId(), row.applicationId(), row.merchantName(), row.materialType(), row.materialName(), row.fileUrl(), row.status(), row.rejectReason(), row.submittedAt(), row.auditedBy(), row.auditedAt());
  }

  private AdminStoreView toStoreView(AdminRepository.StoreRow row) {
    return new AdminStoreView(row.storeId(), row.merchantId(), row.merchantName(), row.storeName(), row.businessType(), row.summary(), row.address(), row.status(), row.businessHoursText(), row.tagText(), row.coverUrl(), row.contactPhone(), row.announcement(), row.updatedAt());
  }

  private AdminUserView toUserView(AdminRepository.UserRow row) {
    return new AdminUserView(row.accountId(), row.userId(), row.nickname(), row.avatarUrl(), row.phone(), row.email(), row.status(), row.addressCount(), row.orderCount(), row.createdAt());
  }

  private AdminDeliveryTaskView toDeliveryTaskView(AdminRepository.DeliveryTaskRow row) {
    return new AdminDeliveryTaskView(row.taskId(), row.orderId(), row.orderNo(), row.storeName(), row.currentStage(), row.currentStageText(), row.autoAdvanceEnabled(), row.pausedAt(), row.abnormalReason(), row.nextTickAt(), row.completedAt(), row.updatedAt());
  }

  private AdminAnnouncementView toAnnouncementView(AdminRepository.AnnouncementRow row) {
    return new AdminAnnouncementView(row.id(), row.title(), row.content(), row.targetClient(), row.coverUrl(), row.status(), row.startAt(), row.endAt(), row.sortOrder(), row.createdBy(), row.updatedAt());
  }

  private AdminConfigView toConfigView(AdminRepository.ConfigRow row) {
    return new AdminConfigView(row.configKey(), row.configValue(), row.remark(), row.updatedAt());
  }

  private AdminAuditLogView toAuditView(AdminRepository.AuditRow row) {
    return new AdminAuditLogView(row.id(), row.actorType(), row.actorId(), row.actionType(), row.targetType(), row.targetId(), row.detail(), row.createdAt());
  }

  private record NextDeliveryStage(String stage, String text, String displayStatus, boolean completed) {}
}
