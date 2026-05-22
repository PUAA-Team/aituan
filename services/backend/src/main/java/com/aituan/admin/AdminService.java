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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class AdminService {
  private static final int DELIVERY_TICK_MINUTES = 3;

  private final AdminRepository adminRepository;
  private final FileStorageService fileStorageService;

  AdminService(AdminRepository adminRepository, FileStorageService fileStorageService) {
    this.adminRepository = adminRepository;
    this.fileStorageService = fileStorageService;
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

  PageResponse<AdminMerchantView> merchants(String keyword, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countMerchants(keyword);
    List<AdminMerchantView> list = adminRepository.listMerchants(keyword, (page - 1) * pageSize, pageSize).stream().map(this::toMerchantView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  PageResponse<AdminStoreView> stores(Long merchantId, String businessType, String status, int page, int pageSize) {
    requireAdmin();
    long total = adminRepository.countStores(merchantId, businessType, status);
    List<AdminStoreView> list = adminRepository.listStores(merchantId, businessType, status, (page - 1) * pageSize, pageSize).stream().map(this::toStoreView).toList();
    return PageResponse.of(list, page, pageSize, total);
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

  private String normalizeAnnouncementStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "draft", "published", "offline" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "公告状态不正确");
    };
  }

  private AdminMerchantView toMerchantView(AdminRepository.MerchantRow row) {
    return new AdminMerchantView(row.merchantId(), row.merchantName(), row.contactName(), row.contactPhone(), row.status(), row.auditStatus(), row.storeCount(), row.itemCount(), row.settledAt());
  }

  private AdminStoreView toStoreView(AdminRepository.StoreRow row) {
    return new AdminStoreView(row.storeId(), row.merchantId(), row.merchantName(), row.storeName(), row.businessType(), row.summary(), row.address(), row.status(), row.coverUrl(), row.contactPhone(), row.announcement(), row.updatedAt());
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
