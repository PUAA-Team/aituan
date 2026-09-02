package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.IdentityClient;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class StubIdentityClient implements IdentityClient {

  @Override
  public Optional<AddressSnapshot> findAddress(long userId, Long addressId) {
    if (addressId != null && addressId <= 0) {
      return Optional.empty();
    }
    return Optional.of(new AddressSnapshot(
        addressId == null ? 1L : addressId,
        userId,
        "李同学",
        "18800001111",
        "北京市",
        "北京市",
        "海淀区",
        "城市广场测试地址 1 号",
        new BigDecimal("116.313600"),
        new BigDecimal("39.982300")));
  }
}
