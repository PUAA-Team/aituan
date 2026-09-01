# B 分工商家商品服务检查反馈

> 对应分支：`microservice-b-merchant-catalog`
> 对应服务：`merchant-catalog-service`
> 对应数据库：`aituan_merchant`
> 检查目的：判断 B 服务是否完成微服务拆分、是否符合统一标准、是否可以和其他服务尤其 C 服务对接。

## 1. 总结论

B 分支已经完成了 `merchant-catalog-service` 的主体拆分，具备开始集成联调的基础。服务代码、商家库 Flyway、外部接口、内部接口、Docker/K8s/CI/Deploy 文件都已经有雏形。

但是，B 分支当前不建议直接合入 `microservices-main`。最大问题是：当前分支不是从 `origin/microservices-main` 正确接出来的，而是更像从 `origin/main` 基线开发后又带入了一部分微服务地基内容。这会导致后续合并时出现工程地基反向覆盖、公共类重复、文档索引丢失和 Gateway/CI 冲突。

建议 B 同学先按本文 P0 项重新整理到 `microservices-main` 基线上，再进入全员集成。

## 2. 已完成内容

### 2.1 服务主体已经完成

B 分支已经实现了独立的 `merchant-catalog-service`，包括：

- 独立 Maven 模块；
- 服务名：`merchant-catalog-service`；
- 服务端口：`8082`；
- 独立数据库口径：`aituan_merchant`；
- Flyway migration；
- seed 脚本；
- 商家、门店、资质、商品、SKU、库存、搜索、履约规则相关代码；
- Gateway 商家商品路由；
- 基础集成测试；
- Dockerfile；
- K8s YAML；
- CI workflow；
- Deploy workflow；
- B 分工交付文档。

### 2.2 数据库拆分范围基本正确

B 的迁移脚本创建了成员 B 归属的 14 张表：

```text
merchant_profile
merchant_store
merchant_delivery_rule
merchant_takeaway_setting
merchant_application
merchant_certification_material
merchant_audit_log
catalog_category
catalog_item
catalog_sku
catalog_item_tag
catalog_item_tag_rel
ops_banner_config
member_recommend_config
```

检查中未发现 `merchant-catalog-service` 直接访问 A/C/D 服务归属表，例如：

```text
iam_*
user_*
order_*
cart
review_*
support_*
complaint_*
sys_*
file_asset
```

这说明 B 服务的数据边界基本符合“每张业务表唯一服务归属，不能跨服务直接查表”的要求。

### 2.3 内部接口基本覆盖统一标准

B 分支已提供统一标准要求的 8 个内部接口：

```text
GET  /internal/stores/{storeId}/snapshot
GET  /internal/merchants/by-account/{accountId}
GET  /internal/catalog/items/{itemId}/snapshot
GET  /internal/stores/{storeId}/fulfillment-rules
POST /internal/catalog/checkout-quote
POST /internal/inventory/deduct
POST /internal/inventory/restore
GET  /internal/metrics/platform/merchants
```

这些接口是 C 服务下单和履约链路后续需要调用的基础，例如门店快照、商品快照、履约规则、结算报价、库存扣减和库存回滚。

### 2.4 外部接口覆盖较完整

B 分支外部接口已经覆盖主要商家商品范围，包括：

- 商家入驻：`/api/open/merchant/applications`；
- 用户端发现页：`/api/app/discovery/**`；
- 用户端位置：`/api/app/location/reverse-geocode`；
- 商家资料：`/api/merchant/profile/me`；
- 商家门店：`/api/merchant/stores/current`；
- 商家资质：`/api/merchant/certification/**`；
- 商家商品目录：`/api/merchant/catalog/**`；
- 平台后台商家/门店/目录：`/api/admin/merchants/**`、`/api/admin/stores/**`、`/api/admin/catalog/**`；
- 商家/后台履约设置：`/api/merchant/trade/stores/**`、`/api/admin/trade/stores/**`；
- 商家驾驶舱：`/api/merchant/ops/dashboard`。

### 2.5 内部调用头和降级已有基础

B 服务已经实现了一些内部调用规范：

- `X-Caller-Service`；
- `X-Request-Id`；
- `X-Service-Token`；
- `Idempotency-Key`；
- 对远程服务调用设置短超时；
- 对部分外部依赖提供 fallback；
- Gateway 禁止外部访问 `/internal/**`。

这些方向是对的，后续需要和 A/C/D 统一细节。

## 3. 必须修正的问题

### P0-1. 分支基线不符合统一标准

检查结果显示 B 分支不是基于 `origin/microservices-main` 最新地基继续开发：

```text
B_CONTAINS_MICROSERVICES_MAIN=false
```

当前远端分支名是：

```text
microservice-b-merchant-catalog
```

而统一分工标准中建议的 B 分支名是类似：

```text
ms/merchant-catalog
```

更关键的是，B 分支和 `origin/microservices-main` 的共同基线不是微服务地基提交。这意味着后续直接 merge 时，可能会把 `microservices-main` 已经做好的工程地基反向覆盖掉。

建议：

1. 从最新 `origin/microservices-main` 新开标准分支；
2. 将 B 服务相关改动迁移或 cherry-pick 过去；
3. 不要直接把当前分支无审查 merge 到集成主线。

这是 B 分支当前最重要的阻塞项。

### P0-2. 误改了 `services/backend`，并复制了公共类

B 分支相对 `microservices-main` 修改了旧单体 `services/backend`，并重新复制了一批公共类，例如：

```text
services/backend/src/main/java/com/aituan/common/api/ApiResponse.java
services/backend/src/main/java/com/aituan/common/api/PageResponse.java
services/backend/src/main/java/com/aituan/common/exception/BusinessException.java
services/backend/src/main/java/com/aituan/common/exception/GlobalExceptionHandler.java
services/backend/src/main/java/com/aituan/common/security/JwtAuthenticationFilter.java
services/backend/src/main/java/com/aituan/common/security/JwtTokenService.java
```

这违反了统一地基口径。

统一标准是：

```text
公共契约统一放在 services/common-contract
backend 和各微服务都复用 common-contract
不要在 backend 里重新复制 common 公共类
```

影响：

- 公共类会重复维护；
- 后续错误码、JWT、统一响应可能不一致；
- 合并时会和 `microservices-main` 冲突；
- 可能破坏旧 backend 作为兼容模块的编译。

建议 B 合入前剔除这些 backend 反向改动。

### P0-3. `docs/ReadMe.md` 删除了 stage-new-3 关键索引

B 分支相对 `microservices-main` 删除了以下文档入口：

```text
后五天微服务拆分实施方案.md
微服务并行拆分分工与统一标准.md
微服务工程地基与适配说明.md
小学期后续看板任务项.md
```

这不符合文档索引要求。

建议：

- 不要覆盖或删除已有 `docs/ReadMe.md` 索引；
- 只追加 B 自己的交付文档入口；
- 合入时以 `microservices-main` 的文档索引为基础。

### P1-1. Deploy workflow 分支名不匹配

B 的 deploy workflow 触发分支写的是：

```yaml
branches:
  - microservices-main
  - ms/merchant-catalog
```

但当前实际远端分支是：

```text
microservice-b-merchant-catalog
```

如果继续使用当前分支名，push 不会触发 deploy workflow。

建议二选一：

1. 按统一标准改用 `ms/merchant-catalog` 分支；
2. 或在 workflow 中加入当前实际分支名。

更推荐第 1 种，统一分支命名。

### P1-2. CI 只覆盖 B 服务，未覆盖 Gateway 路由和迁移 smoke

B 的 CI 当前主要跑 `merchant-catalog-service`，但还缺：

- Gateway merchant route test；
- `/internal/**` 外部访问 403 测试；
- H2 `MODE=MySQL` 空库 Flyway smoke；
- MySQL 8 空库迁移 smoke；
- Docker build 验证；
- K8s dry-run；
- 原始报告 artifact。

作为 B 服务初步测试可以，但还没有达到最终验收标准。

### P1-3. K8s Secret 示例没有补齐

B 的 K8s YAML 引用了：

```text
aituan-merchant-db
aituan-internal-secret
```

但示例 Secret 没有同步补齐对应 key。

后续真实部署时，如果集群里没有这些 Secret，Pod 会启动失败。

建议补充示例说明，至少包括：

```text
aituan-merchant-db.url
aituan-merchant-db.username
aituan-merchant-db.password
aituan-internal-secret.service-token
```

注意：只能写示例 key 和占位值，不能提交真实密码或 token。

### P1-4. 库存幂等目前是 JVM 内存态

B 服务库存扣减/恢复幂等目前使用内存结构保存结果，类似：

```text
ConcurrentHashMap
```

这对单机测试可以，但 K8s 多副本和服务重启后不可靠：

- Pod 重启后幂等记录丢失；
- 同一个请求打到不同 Pod，可能重复扣库存；
- 无法满足生产级幂等要求。

建议：

- 课程演示阶段至少在文档里说明限制；
- 更稳方案是在 `aituan_merchant` 增加库存幂等记录表；
- 表中记录调用方、接口名、幂等键、请求摘要、处理结果、状态和时间。

### P2-1. Flyway seed 使用 MySQL 敏感语法，需要双端验证

B 的 seed 脚本中大量使用：

```sql
ON DUPLICATE KEY UPDATE
VALUES(column)
```

虽然 H2 `MODE=MySQL` 可能支持一部分 MySQL 语法，但这类写法仍需要实际用 H2 和 MySQL 8 都跑一遍验证。

建议补专门的 migration smoke test，而不是只依赖 SpringBootTest 间接启动验证。

### P2-2. 文档中出现同学本机绝对路径

B 文档中出现了类似：

```text
C:/Users/baozh/Downloads/aituan-microservices-main/aituan-microservices-main
```

这不适合作为项目交付文档口径。

建议改成：

```text
<repo-root>
```

或：

```text
项目根目录
```

### P2-3. 文档格式存在 trailing whitespace

`git diff --check` 检查发现 B 文档有行尾空格：

```text
docs/stage-new-3/成员B-商家商品微服务拆分变更记录.md:3: trailing whitespace.
docs/stage-new-3/成员B-商家商品微服务拆分变更记录.md:4: trailing whitespace.
```

建议合入前清理。

## 4. 和 C 服务对接需要关注的接口

C 服务交易下单链路后续会依赖 B 服务这些能力：

```text
GET  /internal/stores/{storeId}/snapshot
GET  /internal/catalog/items/{itemId}/snapshot
GET  /internal/stores/{storeId}/fulfillment-rules
POST /internal/catalog/checkout-quote
POST /internal/inventory/deduct
POST /internal/inventory/restore
```

对接前请重点确认：

1. `checkout-quote` 的商品、SKU、价格、库存字段和 C 服务 DTO 一致；
2. `inventory/deduct` 必须支持幂等，重复请求不能重复扣库存；
3. `inventory/restore` 必须支持订单失败后的库存回滚；
4. 门店快照要包含 C 下单需要的门店名、商家名、经纬度、营业状态等；
5. 履约规则要包含起送价、配送费、配送范围、预计送达时间等；
6. B 服务不能让 C 直接查 `catalog_*` 或 `merchant_*` 表；
7. 内部接口错误码统一使用 `ApiResponse` 格式。

## 5. 建议修复顺序

建议 B 同学按以下顺序处理：

1. 从最新 `origin/microservices-main` 新开标准分支，或把当前分支整理到该基线上；
2. 只迁移 B 自己的改动，不带入 `services/backend/**` 反向修改；
3. 确认 `merchant-catalog-service` 继续依赖 `common-contract`；
4. 恢复 `docs/ReadMe.md` 中已有 stage-new-3 索引，只追加 B 文档；
5. 修正 workflow 分支名；
6. 补 Gateway merchant route test；
7. 补 H2/MySQL Flyway migration smoke；
8. 补 K8s Secret 示例；
9. 处理或记录库存幂等内存态风险；
10. 清理文档 trailing whitespace 和本机绝对路径；

## 6. 当前判断

B 服务主体拆分可以认为“基本完成”，但当前分支不能直接合入。

合入建议：

```text
不建议直接 merge 当前 microservice-b-merchant-catalog，应先以 microservices-main 为基线重放 B 服务改动。
```

对接建议：

```text
可以开始和 C 服务核对内部接口字段，但正式稳定对接前必须先修分支基线、backend 误改、文档索引和 Gateway/测试问题。
```
