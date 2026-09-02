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
- 新增 `deploy/microservices/identity-asset-service/Dockerfile`，identity 服务暴露 8081；B/C/D 服务 Dockerfile 由对应分支维护。
- 新增 `k8s/10-identity-asset-service.yaml`，包含 Deployment、Service、health probe、resources、数据库 Secret、JWT Secret、内部服务 Token。
- 新增 identity-only 微服务流水线：
  - `.github/workflows/microservices-ci.yml`：验证 `common-contract`、`identity-asset-service`、API Gateway identity 路由测试、identity Docker build、identity K8s dry-run、MySQL 8 空库迁移 smoke。
  - `.github/workflows/microservices-deploy.yml`：仅构建、推送、部署 `identity-asset-service`，部署主路径不使用 `|| true` 掩盖失败。

## 部署前准备

真实环境部署前需要先准备数据库与 Secret，仓库只提交示例，不提交真实密钥。

### 数据库

```sql
CREATE DATABASE aituan_identity CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'identity_app'@'%' IDENTIFIED BY 'replace-with-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON aituan_identity.* TO 'identity_app'@'%';
FLUSH PRIVILEGES;
```

生产 JDBC URL 示例：

```text
jdbc:mysql://mysql-host:3306/aituan_identity?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

服务启动时由 Flyway 执行 `database/microservices/identity/migrations` 并写入 `aituan_identity`。

### K8s Secret

Secret key 必须与 `k8s/10-identity-asset-service.yaml` 中的引用保持一致。可参考 `k8s/00-identity-asset-secrets.example.yaml`，或用命令创建：

```bash
kubectl create namespace aituan --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic identity-asset-db-secret \
  --from-literal=url='jdbc:mysql://mysql-host:3306/aituan_identity?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
  --from-literal=username='identity_app' \
  --from-literal=password='replace-with-strong-password'

kubectl -n aituan create secret generic aituan-jwt-secret \
  --from-literal=jwt-secret='replace-with-at-least-32-random-characters'

kubectl -n aituan create secret generic aituan-internal-service-secret \
  --from-literal=token='replace-with-shared-internal-service-token'
```

## 头像 public URL 链路说明

当前 A 服务中的头像上传由 `FileStorageService` 保存到 `AITUAN_UPLOAD_ROOT`，并按 `AITUAN_UPLOAD_PUBLIC_PREFIX` 生成 public URL，默认前缀为 `/api/common/files`。当前 Gateway 中 `/api/common/files/**` 由平台/互动服务路由承接，因此生产头像文件服务归属仍需在全员联调前确认。

本分支不越界实现 D/platform 文件服务。生产方案建议二选一：

1. D/platform 文件服务统一上传并返回 URL，`identity-asset-service` 只保存 URL；
2. `identity-asset-service` 自行提供头像文件服务，并补充 Gateway/static resource/PV 等部署配置。

在团队确认前，当前头像上传链路仅作为 A 分支本地占位能力，不声明为生产完整链路。

## 验证状态

已补充测试与流水线验证项：

- `AuthControllerTest` 覆盖公开认证接口、错误密码、未登录保护。
- `AccountAssetControllerTest` 覆盖用户资料、地址、收藏。
- `UserAssetFeatureControllerTest` 覆盖优惠券、会员、站内消息。
- `AdminControllerTest` 覆盖后台用户、会员等级、优惠券模板基路径接口。
- `IdentityRouteDefinitionTest` 覆盖 Gateway identity 基路径和 `/internal/**` 禁止外部访问规则。
- `IdentityFlywayH2MigrationTest` 覆盖 H2 `MODE=MySQL` 空库迁移。
- `IdentityFlywayMysqlSmokeTest` 通过 `AITUAN_IDENTITY_MYSQL_SMOKE=true` 门控 MySQL 8 空库迁移 smoke。

已做静态检查：

- identity 源码未发现旧包名 `com.aituan.auth/account/member/coupon/message`。
- identity 源码未发现直接访问其他服务业务表。
- identity 数据库拆分脚本未发现跨服务表、外键或 MySQL/H2 明显单方言关键字。

建议合入前执行：

```bash
git diff --check
mvn -B -f services/pom.xml -pl common-contract test
mvn -B -f services/pom.xml -pl identity-asset-service -am test
mvn -B -f services/pom.xml -pl api-gateway test
mvn -B -f services/pom.xml -pl identity-asset-service -am -DskipTests package
docker build -f deploy/microservices/identity-asset-service/Dockerfile -t identity-asset-service:ci .
kubectl apply --dry-run=client --validate=false -f k8s/10-identity-asset-service.yaml
```

MySQL 8 smoke 需要一次性空库：

```bash
AITUAN_IDENTITY_MYSQL_SMOKE=true \
AITUAN_IDENTITY_SMOKE_JDBC_URL='jdbc:mysql://127.0.0.1:3306/aituan_identity_smoke?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false' \
AITUAN_IDENTITY_SMOKE_DB_USERNAME=root \
AITUAN_IDENTITY_SMOKE_DB_PASSWORD=root \
mvn -B -f services/pom.xml -pl identity-asset-service -am -Dtest=IdentityFlywayMysqlSmokeTest test
```
