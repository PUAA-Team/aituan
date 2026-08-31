package com.aituan.identity.admin;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.identity.client.IdentityAuditClient;
import com.aituan.identity.client.TradeMetricsClient;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminService {
  private final AdminRepository adminRepository;
  private final TradeMetricsClient tradeMetricsClient;
  private final IdentityAuditClient identityAuditClient;

  AdminService(AdminRepository adminRepository, TradeMetricsClient tradeMetricsClient, IdentityAuditClient identityAuditClient) {
    this.adminRepository = adminRepository;
    this.tradeMetricsClient = tradeMetricsClient;
    this.identityAuditClient = identityAuditClient;
  }

  AdminProfileView adminProfile() {
    CurrentUser current = requireAdmin();
    return adminRepository.findAdminProfile(current.accountId())
        .map(this::toAdminProfileView)
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
    CurrentUser current = requireAdmin();
    adminRepository.updateUser(accountId, request, normalizeGeneralStatus(defaultValue(request.status(), "normal")));
    identityAuditClient.publish(current.accountId(), "admin_user_update", "iam_account", accountId, "更新用户资料:" + request.nickname());
    return userView(accountId);
  }

  @Transactional
  AdminUserView updateUserStatus(long accountId, AdminStatusRequest request) {
    CurrentUser current = requireAdmin();
    String status = normalizeGeneralStatus(request.status());
    adminRepository.updateUserStatus(accountId, status);
    identityAuditClient.publish(current.accountId(), "admin_user_status", "iam_account", accountId, "更新用户状态:" + status);
    return userView(accountId);
  }

  private AdminUserView userView(long accountId) {
    return adminRepository.findUser(accountId)
        .map(this::toUserView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private CurrentUser requireAdmin() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private AdminProfileView toAdminProfileView(AdminRepository.AdminProfileRow row) {
    return new AdminProfileView(row.accountId(), row.accountNo(), row.accountType(), row.loginName(), row.phone(), row.email(), row.status(), row.createdAt(), row.lastLoginAt());
  }

  private AdminUserView toUserView(AdminRepository.UserRow row) {
    return new AdminUserView(row.accountId(), row.userId(), row.nickname(), row.avatarUrl(), row.phone(), row.email(), row.status(), row.addressCount(), tradeMetricsClient.countUserOrders(row.userId()), row.createdAt());
  }

  private String normalizeGeneralStatus(String status) {
    String value = status == null ? "normal" : status.trim().toLowerCase();
    return switch (value) {
      case "normal", "disabled", "locked" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不支持");
    };
  }

  private String defaultValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
