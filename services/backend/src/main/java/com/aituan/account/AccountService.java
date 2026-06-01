package com.aituan.account;

import com.aituan.common.api.PageResponse;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.file.FileAssetView;
import com.aituan.common.file.FileStorageService;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.discovery.MapDistanceService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class AccountService {
  private final AccountRepository accountRepository;
  private final FileStorageService fileStorageService;
  private final MapDistanceService mapDistanceService;

  AccountService(
      AccountRepository accountRepository,
      FileStorageService fileStorageService,
      MapDistanceService mapDistanceService) {
    this.accountRepository = accountRepository;
    this.fileStorageService = fileStorageService;
    this.mapDistanceService = mapDistanceService;
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
    AddressUpsertRequest resolved = resolveAddressRequest(request);
    boolean makeDefault = Boolean.TRUE.equals(resolved.isDefault()) || accountRepository.countAddresses(userId) == 0;
    if (makeDefault) {
      accountRepository.clearDefaultAddresses(userId);
    }
    Long addressId = accountRepository.insertAddress(userId, resolved, makeDefault);
    return accountRepository.findAddress(userId, addressId)
        .map(this::toAddressView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AddressView updateAddress(long addressId, AddressUpsertRequest request) {
    long userId = CurrentUserContext.required().userId();
    AccountRepository.AddressRow current = requireAddress(userId, addressId);
    AddressUpsertRequest resolved = resolveAddressRequest(request);
    boolean makeDefault = resolved.isDefault() == null ? current.isDefault() : resolved.isDefault();
    if (makeDefault) {
      accountRepository.clearDefaultAddresses(userId);
    }
    accountRepository.updateAddress(userId, addressId, resolved, makeDefault);
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

  private AddressUpsertRequest resolveAddressRequest(AddressUpsertRequest request) {
    boolean hasLongitude = request.longitude() != null;
    boolean hasLatitude = request.latitude() != null;
    if (hasLongitude || hasLatitude) {
      validateLocation(request.longitude(), request.latitude());
      return request;
    }
    MapDistanceService.GeocodeResult geocoded = mapDistanceService.geocode(fullAddress(request));
    if (geocoded == null) {
      return request;
    }
    return new AddressUpsertRequest(
        request.contactName(),
        request.contactPhone(),
        request.province(),
        request.city(),
        request.district(),
        request.detailAddress(),
        geocoded.longitude().doubleValue(),
        geocoded.latitude().doubleValue(),
        request.tagName(),
        request.isDefault(),
        request.deliveryNote());
  }

  private String fullAddress(AddressUpsertRequest request) {
    return request.province().trim() + request.city().trim() + request.district().trim() + request.detailAddress().trim();
  }

  private void validateLocation(Double longitude, Double latitude) {
    if (longitude == null || latitude == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度需要同时填写");
    }
    if (BigDecimal.valueOf(longitude).compareTo(BigDecimal.valueOf(-180)) < 0
        || BigDecimal.valueOf(longitude).compareTo(BigDecimal.valueOf(180)) > 0
        || BigDecimal.valueOf(latitude).compareTo(BigDecimal.valueOf(-90)) < 0
        || BigDecimal.valueOf(latitude).compareTo(BigDecimal.valueOf(90)) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度范围不正确");
    }
  }

  private String normalizeFavoriteType(String favoriteType) {
    return favoriteType == null ? null : favoriteType.trim().toLowerCase();
  }

  private String clean(String value) {
    return value == null ? null : value.trim();
  }
}
