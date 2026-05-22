package com.aituan.account;

import com.aituan.common.api.PageResponse;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.file.FileAssetView;
import com.aituan.common.file.FileStorageService;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class AccountService {
  private final AccountRepository accountRepository;
  private final FileStorageService fileStorageService;

  AccountService(AccountRepository accountRepository, FileStorageService fileStorageService) {
    this.accountRepository = accountRepository;
    this.fileStorageService = fileStorageService;
  }

  AccountProfileView profile() {
    CurrentUser currentUser = CurrentUserContext.required();
    AccountRepository.AccountProfileRow row = accountRepository.findProfile(currentUser.accountId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new AccountProfileView(
        row.accountId(),
        row.userId(),
        row.nickname(),
        row.avatarUrl(),
        row.phone(),
        row.email(),
        row.memberLevelName(),
        row.growthValue(),
        accountRepository.countAddresses(currentUser.userId()),
        accountRepository.countFavorites(currentUser.userId(), null),
        accountRepository.countUnreadMessages(currentUser.userId()));
  }

  @Transactional
  AccountProfileView updateProfile(AccountProfileUpdateRequest request) {
    CurrentUser currentUser = CurrentUserContext.required();
    accountRepository.updateProfile(currentUser.userId(), request.nickname().trim(), clean(request.avatarUrl()));
    return profile();
  }

  @Transactional
  AccountProfileView uploadAvatar(MultipartFile file) {
    CurrentUser currentUser = CurrentUserContext.required();
    FileAssetView asset = fileStorageService.save(file, "avatar");
    accountRepository.updateAvatar(currentUser.userId(), asset.publicUrl());
    return profile();
  }

  List<AddressView> addresses() {
    long userId = CurrentUserContext.required().userId();
    return accountRepository.listAddresses(userId).stream().map(this::toAddressView).toList();
  }

  @Transactional
  AddressView createAddress(AddressUpsertRequest request) {
    long userId = CurrentUserContext.required().userId();
    boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || accountRepository.countAddresses(userId) == 0;
    if (makeDefault) {
      accountRepository.clearDefaultAddresses(userId);
    }
    Long addressId = accountRepository.insertAddress(userId, request, makeDefault);
    return accountRepository.findAddress(userId, addressId)
        .map(this::toAddressView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AddressView updateAddress(long addressId, AddressUpsertRequest request) {
    long userId = CurrentUserContext.required().userId();
    AccountRepository.AddressRow current = requireAddress(userId, addressId);
    boolean makeDefault = request.isDefault() == null ? current.isDefault() : request.isDefault();
    if (makeDefault) {
      accountRepository.clearDefaultAddresses(userId);
    }
    accountRepository.updateAddress(userId, addressId, request, makeDefault);
    if (makeDefault) {
      accountRepository.markAddressDefault(userId, addressId);
    }
    return accountRepository.findAddress(userId, addressId)
        .map(this::toAddressView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  void deleteAddress(long addressId) {
    long userId = CurrentUserContext.required().userId();
    AccountRepository.AddressRow current = requireAddress(userId, addressId);
    accountRepository.deleteAddress(userId, addressId);
    if (current.isDefault()) {
      List<AccountRepository.AddressRow> remain = accountRepository.listAddresses(userId);
      if (!remain.isEmpty()) {
        accountRepository.clearDefaultAddresses(userId);
        accountRepository.markAddressDefault(userId, remain.get(0).id());
      }
    }
  }

  @Transactional
  AddressView setDefaultAddress(long addressId) {
    long userId = CurrentUserContext.required().userId();
    requireAddress(userId, addressId);
    accountRepository.clearDefaultAddresses(userId);
    accountRepository.markAddressDefault(userId, addressId);
    return accountRepository.findAddress(userId, addressId)
        .map(this::toAddressView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<FavoriteView> favorites(String favoriteType, int page, int pageSize) {
    long userId = CurrentUserContext.required().userId();
    String normalizedType = normalizeFavoriteType(favoriteType);
    long total = accountRepository.countFavorites(userId, normalizedType);
    List<FavoriteView> list = accountRepository.listFavorites(userId, normalizedType, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toFavoriteView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  FavoriteView saveFavorite(FavoriteUpsertRequest request) {
    long userId = CurrentUserContext.required().userId();
    FavoriteUpsertRequest normalized = new FavoriteUpsertRequest(
        normalizeFavoriteType(request.favoriteType()),
        request.targetId(),
        request.targetName().trim(),
        request.coverUrl(),
        request.subtitle());
    accountRepository.insertFavorite(userId, normalized);
    return accountRepository.findFavorite(userId, normalized.favoriteType(), normalized.targetId())
        .map(this::toFavoriteView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  void deleteFavorite(String favoriteType, long targetId) {
    long userId = CurrentUserContext.required().userId();
    accountRepository.deleteFavorite(userId, normalizeFavoriteType(favoriteType), targetId);
  }

  private AddressView toAddressView(AccountRepository.AddressRow row) {
    return new AddressView(
        row.id(),
        row.contactName(),
        row.contactPhone(),
        row.province(),
        row.city(),
        row.district(),
        row.detailAddress(),
        row.longitude(),
        row.latitude(),
        row.tagName(),
        row.isDefault(),
        row.deliveryNote(),
        row.createdAt());
  }

  private FavoriteView toFavoriteView(AccountRepository.FavoriteRow row) {
    return new FavoriteView(
        row.id(),
        row.favoriteType(),
        row.targetId(),
        row.targetName(),
        row.coverUrl(),
        row.subtitle(),
        row.createdAt());
  }

  private AccountRepository.AddressRow requireAddress(long userId, long addressId) {
    return accountRepository.findAddress(userId, addressId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private String normalizeFavoriteType(String favoriteType) {
    return favoriteType == null ? null : favoriteType.trim().toLowerCase();
  }

  private String clean(String value) {
    return value == null ? null : value.trim();
  }
}
