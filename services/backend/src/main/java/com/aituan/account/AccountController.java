package com.aituan.account;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/app/account")
@Validated
class AccountController {
  private final AccountService accountService;

  AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/profile")
  ApiResponse<AccountProfileView> profile() {
    return ApiResponse.ok(accountService.profile());
  }

  @PutMapping("/profile")
  ApiResponse<AccountProfileView> updateProfile(@Valid @RequestBody AccountProfileUpdateRequest request) {
    return ApiResponse.ok(accountService.updateProfile(request));
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<AccountProfileView> uploadAvatar(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(accountService.uploadAvatar(file));
  }

  @GetMapping("/addresses")
  ApiResponse<java.util.List<AddressView>> addresses() {
    return ApiResponse.ok(accountService.addresses());
  }

  @PostMapping("/addresses")
  ApiResponse<AddressView> createAddress(@Valid @RequestBody AddressUpsertRequest request) {
    return ApiResponse.ok(accountService.createAddress(request));
  }

  @PutMapping("/addresses/{addressId}")
  ApiResponse<AddressView> updateAddress(@PathVariable long addressId, @Valid @RequestBody AddressUpsertRequest request) {
    return ApiResponse.ok(accountService.updateAddress(addressId, request));
  }

  @PostMapping("/addresses/{addressId}/default")
  ApiResponse<AddressView> setDefaultAddress(@PathVariable long addressId) {
    return ApiResponse.ok(accountService.setDefaultAddress(addressId));
  }

  @DeleteMapping("/addresses/{addressId}")
  ApiResponse<Void> deleteAddress(@PathVariable long addressId) {
    accountService.deleteAddress(addressId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/favorites")
  ApiResponse<PageResponse<FavoriteView>> favorites(
      @RequestParam(required = false) String favoriteType,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(accountService.favorites(favoriteType, page, pageSize));
  }

  @PostMapping("/favorites")
  ApiResponse<FavoriteView> saveFavorite(@Valid @RequestBody FavoriteUpsertRequest request) {
    return ApiResponse.ok(accountService.saveFavorite(request));
  }

  @DeleteMapping("/favorites/{favoriteType}/{targetId}")
  ApiResponse<Void> deleteFavorite(@PathVariable String favoriteType, @PathVariable long targetId) {
    accountService.deleteFavorite(favoriteType, targetId);
    return ApiResponse.ok(null);
  }
}
