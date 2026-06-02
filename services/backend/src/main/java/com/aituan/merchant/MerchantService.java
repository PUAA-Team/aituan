package com.aituan.merchant;

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
class MerchantService {
  private final MerchantRepository merchantRepository;
  private final FileStorageService fileStorageService;
  private final MapDistanceService mapDistanceService;

  MerchantService(
      MerchantRepository merchantRepository,
      FileStorageService fileStorageService,
      MapDistanceService mapDistanceService) {
    this.merchantRepository = merchantRepository;
    this.fileStorageService = fileStorageService;
    this.mapDistanceService = mapDistanceService;
  }

  @Transactional
  MerchantApplicationView submitApplication(MerchantApplicationSubmitRequest request) {
    String businessType = normalizeBusinessType(request.businessType());
    long id = merchantRepository.insertApplication("APP" + System.currentTimeMillis(), request, businessType);
    return merchantRepository.findApplication(id).map(this::toApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
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
    StoreLocation location = resolveLocation(request, store);
    merchantRepository.updateStore(store.id(), request, location.longitude(), location.latitude());
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

  MerchantCertificationView certification() {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    return new MerchantCertificationView(merchant.auditStatus(), merchant.licenseNo(), certificationMaterials());
  }

  List<CertificationMaterialView> certificationMaterials() {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    return merchantRepository.listMaterialsByMerchant(merchant.id()).stream().map(this::toMaterialView).toList();
  }

  @Transactional
  CertificationMaterialView uploadCertificationMaterial(String materialType, String materialName, MultipartFile file) {
    MerchantRepository.MerchantRow merchant = currentMerchant();
    String normalizedType = normalizeMaterialType(materialType);
    FileAssetView asset = fileStorageService.save(file, "merchant-certification");
    long id = merchantRepository.insertMaterial(merchant.id(), normalizedType, cleanOrDefault(materialName, materialTypeLabel(normalizedType)), asset.publicUrl());
    return merchantRepository.findMaterial(id).map(this::toMaterialView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private MerchantRepository.MerchantRow currentMerchant() {
    CurrentUser current = CurrentUserContext.required();
    return merchantRepository.findByAccount(current.accountId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private String normalizeBusinessType(String businessType) {
    String value = businessType == null ? "" : businessType.trim().toLowerCase();
    return switch (value) {
      case "takeaway", "group_buy", "hotel", "entertainment", "movie", "beauty", "ticket", "massage" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "业务类型不正确");
    };
  }

  private String normalizeMaterialType(String materialType) {
    String value = materialType == null ? "" : materialType.trim().toLowerCase();
    return switch (value) {
      case "business_license", "food_license", "identity", "other" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "材料类型不正确");
    };
  }

  private String materialTypeLabel(String materialType) {
    return switch (materialType) {
      case "business_license" -> "营业执照";
      case "food_license" -> "食品经营许可证";
      case "identity" -> "法人身份证明";
      default -> "其他材料";
    };
  }

  private String cleanOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private StoreLocation resolveLocation(MerchantStoreUpdateRequest request, MerchantRepository.StoreRow store) {
    if (request.longitude() != null || request.latitude() != null) {
      validateLocation(request.longitude(), request.latitude());
      return new StoreLocation(request.longitude(), request.latitude());
    }
    MapDistanceService.GeocodeResult geocoded = mapDistanceService.geocode(request.address());
    if (geocoded != null) {
      return new StoreLocation(geocoded.longitude(), geocoded.latitude());
    }
    return new StoreLocation(store.longitude(), store.latitude());
  }

  private void validateLocation(BigDecimal longitude, BigDecimal latitude) {
    if (longitude == null || latitude == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度需要同时填写");
    }
    if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0
        || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度范围不正确");
    }
  }

  private MerchantApplicationView toApplicationView(MerchantRepository.ApplicationRow row) {
    return new MerchantApplicationView(
        row.id(),
        row.applicationNo(),
        row.accountId(),
        row.merchantName(),
        row.contactName(),
        row.contactPhone(),
        row.businessType(),
        row.storeName(),
        row.address(),
        row.status(),
        row.auditRemark(),
        row.submittedAt(),
        row.auditedAt());
  }

  private CertificationMaterialView toMaterialView(MerchantRepository.MaterialRow row) {
    return new CertificationMaterialView(
        row.id(),
        row.merchantId(),
        row.applicationId(),
        row.materialType(),
        row.materialName(),
        row.fileUrl(),
        row.status(),
        row.rejectReason(),
        row.submittedAt(),
        row.auditedAt());
  }

  private record StoreLocation(BigDecimal longitude, BigDecimal latitude) {}

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
        row.longitude(),
        row.latitude(),
        row.updatedAt());
  }
}
