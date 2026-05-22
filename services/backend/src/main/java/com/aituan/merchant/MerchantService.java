package com.aituan.merchant;

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
class MerchantService {
  private final MerchantRepository merchantRepository;
  private final FileStorageService fileStorageService;

  MerchantService(MerchantRepository merchantRepository, FileStorageService fileStorageService) {
    this.merchantRepository = merchantRepository;
    this.fileStorageService = fileStorageService;
  }

  MerchantProfileView profile() {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    List<MerchantStoreView> stores = merchantRepository.listStores(merchant.id()).stream().map(this::toStoreView).toList();
    Long currentStoreId = stores.isEmpty() ? null : stores.get(0).id();
    return new MerchantProfileView(
        merchant.id(),
        merchant.merchantName(),
        merchant.contactName(),
        merchant.contactPhone(),
        merchant.licenseNo(),
        merchant.status(),
        merchant.auditStatus(),
        currentStoreId,
        stores);
  }

  @Transactional
  MerchantProfileView updateProfile(MerchantProfileUpdateRequest request) {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    merchantRepository.updateProfile(merchant.id(), request);
    return profile();
  }

  MerchantStoreView currentStore() {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    return merchantRepository.listStores(merchant.id()).stream()
        .findFirst()
        .map(this::toStoreView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  MerchantStoreView updateCurrentStore(MerchantStoreUpdateRequest request) {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    MerchantRepository.StoreRow store = merchantRepository.listStores(merchant.id()).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    merchantRepository.updateStore(store.id(), request);
    return merchantRepository.findStore(merchant.id(), store.id())
        .map(this::toStoreView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  MerchantStoreView uploadCurrentStoreCover(MultipartFile file) {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    MerchantRepository.StoreRow store = merchantRepository.listStores(merchant.id()).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    FileAssetView asset = fileStorageService.save(file, "store");
    merchantRepository.updateStoreCover(store.id(), asset.publicUrl());
    return merchantRepository.findStore(merchant.id(), store.id())
        .map(this::toStoreView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private MerchantRepository.MerchantRow currentMerchant() {
    CurrentUser current = CurrentUserContext.required();
    return merchantRepository.findByAccount(current.accountId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private MerchantStoreView toStoreView(MerchantRepository.StoreRow row) {
    return new MerchantStoreView(
        row.id(),
        row.merchantId(),
        row.storeName(),
        row.businessType(),
        row.summary(),
        row.address(),
        row.rating(),
        row.monthlySales(),
        row.avgPrice(),
        row.status(),
        row.businessHoursText(),
        row.tagText(),
        row.coverUrl(),
        row.contactPhone(),
        row.announcement(),
        row.updatedAt());
  }
}
