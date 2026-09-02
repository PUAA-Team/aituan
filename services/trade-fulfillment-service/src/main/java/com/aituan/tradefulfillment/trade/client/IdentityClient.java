package com.aituan.tradefulfillment.trade.client;

import java.math.BigDecimal;
import java.util.Optional;

public interface IdentityClient {

  Optional<AddressSnapshot> findAddress(long userId, Long addressId);

  record AddressSnapshot(
      Long id,
      Long userId,
      String contactName,
      String contactPhone,
      String province,
      String city,
      String district,
      String detailAddress,
      BigDecimal longitude,
      BigDecimal latitude) {}
}
