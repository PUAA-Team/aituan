# 成员 B：商家与商品微服务拆分变更记录

> 工作日期：2026-08-31
> 工作目录：`<repo-root>`
> Git 状态：经用户确认，先在本地直接修改并记录操作，最后推送时新建分支 `ms/merchant-catalog`。

## 1. 执行标准

- 主标准：`docs/stage-new-3/微服务并行拆分分工与统一标准.md`
- 工程地基：`docs/stage-new-3/微服务工程地基与适配说明.md`
- 接口归属：`docs/stage-new-3/微服务接口清单.md`
- 数据库归属：`docs/stage-new-3/数据表归属方案.md`

## 2. 成员 B 目标边界

- 服务名：`merchant-catalog-service`
- 端口：`8082`
- Java 根包：沿用当前工程地基 `com.aituan.merchantcatalog`
- 逻辑库：`aituan_merchant`
- 服务账号：`aituan_merchant_svc`
- 表范围：`merchant_profile`、`merchant_store`、`merchant_delivery_rule`、`merchant_takeaway_setting`、`merchant_application`、`merchant_certification_material`、`merchant_audit_log`、`catalog_category`、`catalog_item`、`catalog_sku`、`catalog_item_tag`、`catalog_item_tag_rel`、`ops_banner_config`、`member_recommend_config`

## 3. 操作时间线

- 2026-08-31：读取成员 B 拆分标准、工程地基说明、接口清单、数据表归属方案。
- 2026-08-31：确认当前目录不是 Git 仓库，无法本地创建分支；用户确认通过记录变更保证过程可追溯。
- 2026-08-31：新建本变更记录文件。

## 4. 文件变更清单

### 已新增

- `docs/stage-new-3/成员B-商家商品微服务拆分变更记录.md`：记录成员 B 拆分过程、文件变更、测试结果和已知事项。
- `database/microservices/merchant/migrations/V001__init_merchant_catalog.sql`：拆出 `aituan_merchant` 逻辑库的 14 张成员 B 归属表，避免跨服务表和跨库外键。
- `database/microservices/merchant/seeds/R__seed_merchant_catalog_demo.sql`：拆出商家、门店、配送规则、接单设置、分类、商品、SKU、推荐位 demo seed，保持幂等。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/config/InternalApiConfig.java`：新增 `/internal/**` 服务间调用头与服务凭证校验。
- `deploy/microservices/merchant-catalog-service/Dockerfile`：新增商家商品服务独立镜像构建文件。
- `k8s/11-merchant-catalog-service.yaml`：新增商家商品服务 Deployment/Service、健康检查、资源限制和数据库 Secret 引用。
- `.github/workflows/microservices-ci.yml`：新增微服务 CI 工作流，当前矩阵包含 `merchant-catalog-service`。
- `.github/workflows/microservices-deploy.yml`：新增商家商品服务测试、制镜像、推送 GHCR 和可选 K8s 部署工作流。

### 已修改

- `services/merchant-catalog-service/pom.xml`：加入 JDBC、Flyway、MySQL、H2、Security、Actuator、OpenAPI 与 `common-contract` 依赖，并把商家库 migration/seed 作为运行资源打包。
- `services/merchant-catalog-service/src/main/resources/application.yml`：配置端口 8082、`aituan_merchant` 数据源、Flyway 路径、服务间 URL、内部服务 token、地图与文件公共前缀。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/MerchantCatalogControllers.java`：承接成员 B 外部 65 个操作范围，新增商家驾驶舱 `/api/merchant/ops/dashboard`，并提供 8 个 `/internal/**` 契约接口。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/MerchantCatalogService.java`：迁入商家入驻、门店、资质、目录、搜索、首页、配送规则、接单设置、商品报价、库存扣减/恢复、平台指标和商家看板聚合逻辑。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/MerchantCatalogRepository.java`：只访问成员 B 归属表，查询/写入边界限定在 `merchant_profile`、`merchant_store`、`merchant_delivery_rule`、`merchant_takeaway_setting`、`merchant_application`、`merchant_certification_material`、`merchant_audit_log`、`catalog_category`、`catalog_item`、`catalog_sku`、`catalog_item_tag`、`catalog_item_tag_rel`、`ops_banner_config`、`member_recommend_config`。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/MerchantCatalogSupport.java`：实现文件 URL 生成、地图/距离降级、对身份/交易/平台内部接口的带 `X-Request-Id`、`X-Caller-Service`、`X-Service-Token`、`Idempotency-Key` 调用适配。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/dto/MerchantCatalogDtos.java`：补齐公开 API、内部契约、商家看板和跨服务调用 DTO。
- `services/merchant-catalog-service/src/main/java/com/aituan/merchantcatalog/config/SecurityConfig.java`：配置公开、商家、后台和内部接口访问规则。
- `services/merchant-catalog-service/src/test/java/com/aituan/merchantcatalog/MerchantCatalogServiceIntegrationTest.java`：覆盖 H2 空库迁移、首页、内部快照、报价、库存幂等、内部接口鉴权、商家 token 和平台指标。
- `services/api-gateway/src/main/resources/application.yml`：新增/确认成员 B 负责路由，包含 `/api/open/merchant/**`、`/api/app/discovery/**`、`/api/app/location/**`、`/api/merchant/profile/**`、`/api/merchant/stores/**`、`/api/merchant/catalog/**`、`/api/merchant/ops/dashboard`、`/api/merchant/trade/stores/**`、`/api/admin/merchants/**`、`/api/admin/stores/**`、`/api/admin/catalog/**`、`/api/admin/trade/stores/**`，并保留 `/internal/**` 外部拒绝规则。

### 保留未改

- `services/backend/`：按标准保留单体基线，不删除。
- 原单体 CI/CD：按标准不直接覆盖，新增微服务流水线并行存在。

## 5. 测试记录

- 2026-08-31 15:29：执行 `mvn -f services/pom.xml -pl merchant-catalog-service -am -Dmaven.repo.local=D:/aituan_cache/m2 test`，结果 `BUILD SUCCESS`，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-08-31 15:40：执行 `mvn -f services/pom.xml -pl merchant-catalog-service,api-gateway -am -Dmaven.repo.local=D:/aituan_cache/m2 test`，结果 `BUILD SUCCESS`，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-08-31 15:50：推送前最终执行 `mvn -f services/pom.xml -pl merchant-catalog-service,api-gateway -am -Dmaven.repo.local=D:/aituan_cache/m2 test`，结果 `BUILD SUCCESS`，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 空库迁移验证：上述测试启动 `jdbc:h2:mem:aituan_merchant;MODE=MySQL`，Flyway 成功应用 `V001__init_merchant_catalog` 与 `R__seed_merchant_catalog_demo`。

## 6. 自检结论

- 服务名、端口、逻辑库：符合 `merchant-catalog-service` / `8082` / `aituan_merchant`。
- 外部接口：成员 B 清单中的商家入驻、发现、位置、商家资料、门店、目录、商家交易门店规则、后台商家/门店/目录接口已由商家商品服务承接。
- 内部接口：已提供门店快照、商家账号映射、商品快照、履约规则、商品报价、库存扣减、库存恢复、平台商家指标 8 个契约。
- 数据库边界：源码检查未发现成员 B 服务直接访问身份、交易、互动平台归属表；库存修改只写 `catalog_sku`。
- 幂等：库存扣减/恢复要求 `Idempotency-Key`，重复同 key 返回已保存结果。
- Gateway：成员 B 路由已配置，`/internal/**` 外部访问由 Gateway 403 拒绝。
- Docker/K8s/CI：成员 B 独立 Dockerfile、K8s YAML、微服务 CI/Deploy 工作流已补齐。

## 7. 已知事项

- 分支基线：当前分支 `microservice-b-merchant-catalog` 不是从 `origin/microservices-main` 接出，合入前需以 `microservices-main` 为基线重放 B 服务改动（P0-1），不要直接 merge。
- `merchant-catalog-service` 的商家驾驶舱按标准不直连订单/评价/客服表，依赖交易与平台内部指标接口；当依赖服务不可用时返回 0/空数据降级。
- 当前只完成成员 B 范围；全链路 E2E 需要 A/C/D 服务与 Gateway 一起启动后从 Gateway 执行。

## 8. 检查反馈整改记录（2026-09-01）

对照《B 分工商家商品服务检查反馈.md》逐项整改如下：

- **P0-2 公共类去重**：删除 `services/backend` 下与 `common-contract` 逐字节重复的 15 个公共类（`common/api`、`common/exception`、`common/security`、`common/enums`、`common/web`），并在 `services/backend/pom.xml` 增加 `com.aituan:common-contract` 依赖；`backend` 仅保留自身专属的 `common/config`、`common/file`、`common/jdbc`。验证：`mvn -f services/pom.xml -pl backend -am compile` 通过（114 源文件）。
- **P0-3 文档索引**：`docs/ReadMe.md` 补回 stage-new-3 文档索引条目（微服务拆分分工标准、工程地基、实施方案、划分设计、接口清单、数据表归属、划分依据、看板任务、成员 B 变更记录）。
- **P1-1 部署分支名**：`microservices-deploy.yml` 触发分支补充当前实际分支名 `microservice-b-merchant-catalog`（后续以 P0-1 统一为 `ms/merchant-catalog` 后回退）。
- **P1-2 测试补齐**：新增 `services/api-gateway` 路由测试（`/internal/**` 403 拒绝、商家路由注册断言，3 用例通过）；新增 `MerchantCatalogFlywayMigrationSmokeTest`（H2 空库迁移 + seed 幂等重复执行，校验 15 张表）；`microservices-ci.yml` 矩阵加入 `api-gateway`，并补 Docker build 与 `kubectl dry-run` 校验步骤。
- **P1-3 Secret 示例**：`k8s/secret.example.yaml` 补齐 `aituan-merchant-db`（url/username/password）与 `aituan-internal-secret`（service-token）示例 key 与占位值。
- **P1-4 库存幂等落库**：新增迁移 `V002__inventory_idempotency.sql` 建表 `inventory_idempotency_record`（`caller_service + api_action + idempotency_key` 唯一键），库存扣减/恢复幂等从 JVM `ConcurrentHashMap` 改为数据库记录 + JSON 结果回放。
- **P2-1 seed 双端语法**：`R__seed_merchant_catalog_demo.sql` 的 `ON DUPLICATE KEY UPDATE ... VALUES(col)` 在 H2 `MODE=MySQL` 下经 smoke test 验证可正常迁移且重复执行幂等；仍建议 MySQL 8 真库跑一次（CI 已预留 docker mysql 步骤可补齐）。
- **P2-2/P2-3 文档清理**：变更记录中的本机绝对路径改为 `<repo-root>`，清理行尾空格（`git diff --check` 通过）。

### 整改后测试结果

- `merchant-catalog-service`：`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`（12 单测 + 6 集成 + 1 迁移 smoke）。
- `api-gateway`：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `backend`：`compile` 通过。

### 补充：Service 单元测试

新增 `MerchantCatalogServiceUnitTest.java`（Mockito），覆盖：库存扣减/回滚及落库幂等（含幂等键缺失、SKU 不匹配、库存不足、重复 key 回放）、结算报价（金额合计、商品跨店、商品下架、库存不足）、内部门店快照及资源缺失异常。补齐了此前只有集成测试、缺少 Service 单元测试的缺口。
