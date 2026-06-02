package com.aituan.admin;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@Validated
class AdminController {
  private final AdminService adminService;

  AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/dashboard")
  ApiResponse<AdminDashboardView> dashboard() {
    return ApiResponse.ok(adminService.dashboard());
  }

  @GetMapping("/account/profile")
  ApiResponse<AdminProfileView> adminProfile() {
    return ApiResponse.ok(adminService.adminProfile());
  }

  @GetMapping("/merchants")
  ApiResponse<PageResponse<AdminMerchantView>> merchants(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.merchants(keyword, page, pageSize));
  }

  @PostMapping("/merchants")
  ApiResponse<AdminMerchantView> createMerchant(@Valid @RequestBody AdminMerchantUpsertRequest request) {
    return ApiResponse.ok(adminService.createMerchant(request));
  }

  @PutMapping("/merchants/{merchantId}")
  ApiResponse<AdminMerchantView> updateMerchant(@PathVariable long merchantId, @Valid @RequestBody AdminMerchantUpsertRequest request) {
    return ApiResponse.ok(adminService.updateMerchant(merchantId, request));
  }

  @PostMapping("/merchants/{merchantId}/status")
  ApiResponse<AdminMerchantView> updateMerchantStatus(@PathVariable long merchantId, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(adminService.updateMerchantStatus(merchantId, request));
  }

  @GetMapping("/merchants/applications")
  ApiResponse<PageResponse<AdminMerchantApplicationView>> merchantApplications(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.merchantApplications(status, page, pageSize));
  }

  @PostMapping("/merchants/applications/{id}/approve")
  ApiResponse<AdminMerchantApplicationView> approveMerchantApplication(@PathVariable long id, @RequestBody(required = false) AdminMerchantApplicationAuditRequest request) {
    return ApiResponse.ok(adminService.approveMerchantApplication(id, request));
  }

  @PostMapping("/merchants/applications/{id}/reject")
  ApiResponse<AdminMerchantApplicationView> rejectMerchantApplication(@PathVariable long id, @RequestBody(required = false) AdminMerchantApplicationAuditRequest request) {
    return ApiResponse.ok(adminService.rejectMerchantApplication(id, request));
  }

  @GetMapping("/merchants/certification-materials")
  ApiResponse<PageResponse<AdminCertificationMaterialView>> certificationMaterials(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.certificationMaterials(status, page, pageSize));
  }

  @PostMapping("/merchants/certification-materials/{id}/status")
  ApiResponse<AdminCertificationMaterialView> updateCertificationMaterialStatus(@PathVariable long id, @Valid @RequestBody AdminCertificationMaterialAuditRequest request) {
    return ApiResponse.ok(adminService.updateCertificationMaterialStatus(id, request));
  }

  @GetMapping("/stores")
  ApiResponse<PageResponse<AdminStoreView>> stores(
      @RequestParam(required = false) Long merchantId,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.stores(merchantId, businessType, status, page, pageSize));
  }

  @PostMapping("/stores")
  ApiResponse<AdminStoreView> createStore(@Valid @RequestBody AdminStoreUpsertRequest request) {
    return ApiResponse.ok(adminService.createStore(request));
  }

  @PutMapping("/stores/{storeId}")
  ApiResponse<AdminStoreView> updateStore(@PathVariable long storeId, @Valid @RequestBody AdminStoreUpsertRequest request) {
    return ApiResponse.ok(adminService.updateStore(storeId, request));
  }

  @PostMapping("/stores/{storeId}/status")
  ApiResponse<AdminStoreView> updateStoreStatus(@PathVariable long storeId, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(adminService.updateStoreStatus(storeId, request));
  }

  @PostMapping(value = "/stores/{storeId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<AdminStoreView> uploadStoreCover(@PathVariable long storeId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(adminService.uploadStoreCover(storeId, file));
  }

  @GetMapping("/users")
  ApiResponse<PageResponse<AdminUserView>> users(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.users(keyword, page, pageSize));
  }

  @PutMapping("/users/{accountId}")
  ApiResponse<AdminUserView> updateUser(@PathVariable long accountId, @Valid @RequestBody AdminUserUpdateRequest request) {
    return ApiResponse.ok(adminService.updateUser(accountId, request));
  }

  @PostMapping("/users/{accountId}/status")
  ApiResponse<AdminUserView> updateUserStatus(@PathVariable long accountId, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(adminService.updateUserStatus(accountId, request));
  }

  @GetMapping("/delivery/tasks")
  ApiResponse<PageResponse<AdminDeliveryTaskView>> deliveryTasks(
      @RequestParam(required = false) String stage,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.deliveryTasks(stage, page, pageSize));
  }

  @GetMapping("/delivery/tasks/{taskId}")
  ApiResponse<AdminDeliveryTaskView> deliveryTask(@PathVariable long taskId) {
    return ApiResponse.ok(adminService.deliveryTask(taskId));
  }

  @PostMapping("/delivery/tasks/{taskId}/advance")
  ApiResponse<AdminDeliveryTaskView> advanceDeliveryTask(@PathVariable long taskId, @RequestBody(required = false) AdminDeliveryActionRequest request) {
    return ApiResponse.ok(adminService.advanceDeliveryTask(taskId, request));
  }

  @PostMapping("/delivery/tasks/{taskId}/pause")
  ApiResponse<AdminDeliveryTaskView> pauseDeliveryTask(@PathVariable long taskId) {
    return ApiResponse.ok(adminService.pauseDeliveryTask(taskId));
  }

  @PostMapping("/delivery/tasks/{taskId}/resume")
  ApiResponse<AdminDeliveryTaskView> resumeDeliveryTask(@PathVariable long taskId) {
    return ApiResponse.ok(adminService.resumeDeliveryTask(taskId));
  }

  @PostMapping("/delivery/tasks/{taskId}/abnormal")
  ApiResponse<AdminDeliveryTaskView> markDeliveryAbnormal(@PathVariable long taskId, @RequestBody(required = false) AdminDeliveryActionRequest request) {
    return ApiResponse.ok(adminService.markDeliveryAbnormal(taskId, request));
  }

  @GetMapping("/delivery/settings")
  ApiResponse<AdminDeliverySettingView> deliverySettings() {
    return ApiResponse.ok(adminService.deliverySettings());
  }

  @PostMapping("/delivery/settings")
  ApiResponse<AdminDeliverySettingView> updateDeliverySettings(@Valid @RequestBody AdminDeliverySettingRequest request) {
    return ApiResponse.ok(adminService.updateDeliverySettings(request));
  }

  @GetMapping("/announcements")
  ApiResponse<PageResponse<AdminAnnouncementView>> announcements(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.announcements(status, page, pageSize));
  }

  @PostMapping("/announcements")
  ApiResponse<AdminAnnouncementView> createAnnouncement(@Valid @RequestBody AdminAnnouncementUpsertRequest request) {
    return ApiResponse.ok(adminService.createAnnouncement(request));
  }

  @PutMapping("/announcements/{id}")
  ApiResponse<AdminAnnouncementView> updateAnnouncement(@PathVariable long id, @Valid @RequestBody AdminAnnouncementUpsertRequest request) {
    return ApiResponse.ok(adminService.updateAnnouncement(id, request));
  }

  @PostMapping("/announcements/{id}/status")
  ApiResponse<AdminAnnouncementView> updateAnnouncementStatus(@PathVariable long id, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(adminService.updateAnnouncementStatus(id, request));
  }

  @GetMapping("/configs")
  ApiResponse<List<AdminConfigView>> configs() {
    return ApiResponse.ok(adminService.configs());
  }

  @PutMapping("/configs/{key}")
  ApiResponse<List<AdminConfigView>> updateConfig(@PathVariable String key, @Valid @RequestBody AdminConfigUpdateRequest request) {
    return ApiResponse.ok(adminService.updateConfig(key, request));
  }

  @GetMapping("/audit-logs")
  ApiResponse<PageResponse<AdminAuditLogView>> auditLogs(
      @RequestParam(required = false) String actionType,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(adminService.auditLogs(actionType, page, pageSize));
  }
}
