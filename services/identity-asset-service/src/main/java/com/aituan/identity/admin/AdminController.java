package com.aituan.identity.admin;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Validated
class AdminController {
  private final AdminService adminService;

  AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/account/profile")
  ApiResponse<AdminProfileView> adminProfile() {
    return ApiResponse.ok(adminService.adminProfile());
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
}
