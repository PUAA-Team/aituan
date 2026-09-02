package com.aituan.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InternalIdentityServiceTest {
  @Mock private InternalIdentityRepository repository;
  @Mock private PasswordEncoder passwordEncoder;

  private InternalIdentityService service;

  @BeforeEach
  void setUp() {
    service = new InternalIdentityService(repository, passwordEncoder, "test-default-password");
  }

  @Test
  void merchantProvisionShouldReturnExistingAccountIdempotently() {
    when(repository.findAccountByLogin("APP-1001"))
        .thenReturn(Optional.of(new InternalIdentityRepository.AccountRow(9001L, "M9001", "APP-1001", "normal")));

    MerchantAccountProvisionView result = service.provisionMerchantAccount(
        new MerchantAccountProvisionRequest("APP-1001", "测试商家", "联系人", "13800000000"),
        "merchant-catalog-service:merchant-account-provision:APP-1001:v1");

    assertThat(result.success()).isTrue();
    assertThat(result.accountId()).isEqualTo(9001L);
    assertThat(result.message()).contains("原结果");
    verify(repository, never()).insertMerchantAccount(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void merchantProvisionShouldFailClosedWhenInitialPasswordIsMissing() {
    service = new InternalIdentityService(repository, passwordEncoder, "");
    when(repository.findAccountByLogin("APP-1002")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.provisionMerchantAccount(
        new MerchantAccountProvisionRequest("APP-1002", "测试商家", "联系人", "13800000001"),
        "merchant-catalog-service:merchant-account-provision:APP-1002:v1"))
        .isInstanceOf(com.aituan.common.exception.BusinessException.class)
        .hasMessage("商家初始密码未配置");

    verify(repository, never()).insertMerchantAccount(
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void duplicateGrowthSourceShouldNotChangeGrowthAgain() {
    InternalIdentityRepository.UserSummaryRow user = new InternalIdentityRepository.UserSummaryRow(
        5001L, 1001L, "用户", null, null, null, "normal", "白银会员", 120);
    when(repository.findUserSummary(5001L)).thenReturn(Optional.of(user));
    when(repository.insertGrowthLog(5001L, "order", 9001L, 5, "订单完成")).thenReturn(false);

    CouponCommandResult result = service.addGrowth(
        5001L,
        new MemberGrowthRequest("order", 9001L, 5, "订单完成"),
        "trade-fulfillment-service:growth:order-9001:v1");

    assertThat(result.status()).isEqualTo("unchanged");
    verify(repository, never()).changeGrowth(5001L, 5);
  }
}
