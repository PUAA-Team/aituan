# 分工A：账号与用户资产服务微服务拆分交付说明

> 分支：`ms/identity-asset`  
> 服务：`identity-asset-service`  
> 数据库：`aituan_identity`  
> 端口：`8081`

## 已完成范围

- 新增 `services/identity-asset-service`，包名统一为 `com.aituan.identity`。
- 从单体迁移账号认证、用户资料、地址、收藏、会员、优惠券、站内消息能力。
- 后台只抽取成员 A 负责接口：
  - `GET /api/admin/account/profile`
  - `GET /api/admin/users`
  - `PUT /api/admin/users/{accountId}`
  - `POST /api/admin/users/{accountId}/status`
  - `GET/POST/PUT /api/admin/operation/member-levels/**`
  - `GET/POST/PUT /api/admin/operation/coupon-templates/**`
- 新增 12 个内部接口：
  - `GET /internal/users/{userId}/summary`
  - `GET /internal/users/{userId}/addresses/{addressId}/snapshot`
  - `GET /internal/users/{userId}/home-summary`
  - `GET /internal/users/{userId}/preference-signals`
  - `POST /internal/coupons/quote`
  - `POST /internal/coupons/{couponId}/use`
  - `POST /internal/coupons/{couponId}/release`
  - `POST /internal/members/{userId}/growth`
  - `POST /internal/messages`
  - `POST /internal/merchant-accounts/provision`
  - `POST /internal/merchant-accounts/{accountId}/deactivate`
  - `GET /internal/metrics/platform/users`

## 微服务边界处理

- `identity-asset-service` 只访问 identity 表，不直接查 `order_main`、`merchant_store`、`catalog_item`、`review_record`、`sys_audit_log`、`file_asset` 等其他服务表。
- 用户资料中的订单数改为通过 `TradeMetricsClient` 调用交易服务内部接口，失败降级为 0。
- 后台会员/优惠券/用户操作审计改为 `IdentityAuditClient` 调用平台服务 `/internal/audit-logs`，不直接写平台库。
- 用户头像上传只保存本地对象与 URL，不写平台服务的 `file_asset` 表。
- 内部接口校验 `X-Request-Id`、`X-Caller-Service`，若配置 `AITUAN_INTERNAL_SERVICE_TOKEN` 还会校验 `X-Service-Token`。
- 内部写接口要求 `Idempotency-Key`。

## 数据库拆分

新增 `database/microservices/identity`：

- `migrations/V001__identity_iam_user_asset.sql`
  - `iam_account`
  - `iam_verification_code`
  - `iam_role`
  - `iam_permission`
  - `iam_account_role`
  - `iam_role_permission`
  - `user_profile`
  - `user_address`
  - `user_favorite`
- `migrations/V002__identity_station_message.sql`
  - `support_station_message`
- `migrations/V003__identity_member_coupon.sql`
  - `member_level`
  - `coupon_template`
  - `user_coupon`
- `migrations/V004__identity_member_weekly_coupon.sql`
  - `member_weekly_coupon_rule`
  - `member_weekly_coupon_batch`
  - `member_weekly_coupon_issue`
- `migrations/V005__identity_member_growth_log.sql`
  - `member_growth_log`

Seed 拆分为：

- `seeds/R__identity_core_accounts.sql`
- `seeds/R__identity_member_coupon_base.sql`
- `seeds/R__identity_member_weekly_coupon.sql`
- `seeds/R__identity_messages_favorites.sql`

## Gateway / 部署 / CI

- `services/api-gateway/src/main/resources/application.yml` 已补标准路由，并最优先禁止 `/internal/**` 外部访问。
- 新增 `deploy/microservices/*/Dockerfile`，其中 identity 服务暴露 8081。
- 新增 `k8s/10-identity-asset-service.yaml`，包含 Deployment、Service、health probe、resources、数据库 Secret、JWT Secret、内部服务 Token。
- 新增微服务矩阵流水线：
  - `.github/workflows/microservices-ci.yml`
  - `.github/workflows/microservices-deploy.yml`

## 验证状态

已做静态检查：

- identity 源码未发现旧包名 `com.aituan.auth/account/member/coupon/message`。
- identity 源码未发现直接访问其他服务业务表。
- identity 数据库拆分脚本未发现跨服务表、外键或 MySQL/H2 明显单方言关键字。

未能执行 Maven 测试：当前环境 `mvn` 不在 PATH，且仓库未提供 `mvnw`。待本机安装 Maven 或配置 PATH 后执行：

```powershell
mvn -f services/pom.xml -pl identity-asset-service -am -Dmaven.repo.local=D:/aituan_cache/m2 test
```
