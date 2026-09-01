# 成员 D：互动与平台微服务拆分交付说明

## 1. 交付结论

成员 D 的实现位于 `services/engagement-platform-service`，服务名、端口、数据库和分支分别为：

| 项目 | 值 |
| --- | --- |
| 服务名 | `engagement-platform-service` |
| 端口 | `8084` |
| 数据库 | `aituan_platform` |
| 数据库账号 | `aituan_platform_svc` |
| Java 根包 | `com.aituan.engagementplatform` |
| 开发分支 | `ms/engagement-platform` |

实现严格以《微服务并行拆分分工与统一标准》和《微服务接口清单》2.4 节为准。评价、投诉、客服、AI、文件、公告、配置、审计、治理看板及平台配送设置已经从单体边界迁入独立服务；公开 URL 保持不变，供 Gateway 透传。

## 2. 后端拆分结果

| 包 | 职责 |
| --- | --- |
| `interaction` | 用户评价、点赞、举报，商家回复，平台审核；评价成功后补偿标记订单已评价 |
| `complaint` | 用户投诉与补充材料，商家查看，平台受理、处理和关闭 |
| `support` | 用户/商家/平台客服会话、消息、转人工和平台介入、自动回复规则 |
| `ai` | AI 会话、消息和仅限平台域的技能编排 |
| `file` | 文件元数据、上传与读取、Demo 图片 |
| `platform` | 公告、系统配置、配送模拟设置、审计、治理指标和 4 个内部接口 |
| `client` | 账号资产、商家商品、交易履约三个服务的内部 REST 客户端与降级策略 |
| `config` | JWT、内部服务令牌、角色路由和 CORS |

服务实际暴露《微服务接口清单》2.4 节的 59 个公开操作，不增不减；`PublicEndpointInventoryTest` 会把 Spring 实际路由与 59 项标准清单逐项比较。另提供 4 个内部操作：

```text
GET  /internal/reviews/stores/{storeId}/summary
GET  /internal/metrics/stores/{storeId}/engagement
GET  /internal/metrics/platform/governance
POST /internal/audit-logs
```

内部请求必须携带非空的 `X-Caller-Service`、`X-Service-Token` 和 `X-Request-Id`；内部写操作还必须携带 `Idempotency-Key`。审计写入以 `(caller_service, idempotency_key)` 唯一约束实现重复提交幂等。

## 3. 数据库拆分

### 3.1 独占表

`aituan_platform` 只包含以下 18 张业务表：

```text
review_record                  review_reply
review_helpful                 review_report
review_audit_log               support_session
support_message                merchant_support_auto_reply_rule
complaint_ticket               complaint_log
ai_assistant_conversation      ai_assistant_message
platform_announcement          sys_config
sys_dict                       sys_request_log
sys_audit_log                  file_asset
```

建表迁移在 `src/main/resources/db/migration/platform/V1__platform_schema.sql`，初始化数据在 `src/main/resources/db/seed/platform/R__platform_seed.sql`。迁移测试从空库执行 Flyway，并使用 JDBC 元数据断言业务表恰好为上述 18 张，防止其他服务的表被误带入。

### 3.2 跨域数据处理

平台库不查询或 JOIN 用户、门店、商品、订单表。评价、投诉和客服在创建时通过内部接口获取必要数据，并在本地保存 `user_id/user_nickname`、`order_id/order_no/order_title`、`store_id/store_name/merchant_id` 等快照。因此依赖服务短暂不可用时，已经创建的评价、投诉和会话仍可查询与展示。

文件二进制保存在配置的上传目录，`file_asset` 保存元数据；其他服务只保存文件 URL。

### 3.3 初始化数据库

数据库管理员首次部署时创建逻辑库和最小权限账号，具体命令见 `deploy/microservices/engagement-platform-service/README.md`。服务账号只授予 `aituan_platform.*` 权限，不能访问 `aituan_identity`、`aituan_merchant` 或 `aituan_trade`。

## 4. 跨服务调用、重试与降级

| 依赖 | 调用内容 | 处理策略 |
| --- | --- | --- |
| 账号与用户资产 | 用户摘要、站内消息、成长值、平台用户指标 | GET 最多重试 1 次；消息/成长值失败不回滚主业务 |
| 商家与商品 | 门店/商品快照、账号到商家映射、平台商家指标 | 创建时失败返回可重试业务错误；已有本地快照不受影响 |
| 交易与履约 | 订单快照、评价资格、标记订单已评价、订单指标 | 发布评价前必须校验；写命令不自动重试，由补偿任务使用固定幂等键重试 |

内部 GET 的默认连接/读取超时为 `1000ms/1500ms`，只在连接失败、读取超时或 5xx 时额外重试一次；404 映射为资源不存在。内部 POST 默认不自动重试。

评价先以 `order_id` 唯一键写入平台库，再调用交易服务标记订单已评价。如果第二步失败，`review_record.order_marked/order_mark_attempts/order_mark_last_error` 记录补偿状态；后台任务按相同 `review-{reviewId}` 幂等键重试，最多 10 次。此设计保证评价不重复、订单标记不重复。

治理总览并行/依次获取其他三服务的平台指标；任一依赖失败时保留本服务治理指标，缺失指标返回空对象并标记降级，不阻塞平台服务启动。

## 5. 安全与服务边界

- `/api/app/**`、`/api/merchant/**`、`/api/admin/**` 分别要求 USER、MERCHANT、ADMIN 角色。
- `/api/common/**`、健康检查和 OpenAPI 文档按现有公开契约放行。
- `/internal/**` 由独立内部认证过滤器校验调用方白名单和共享令牌。
- `ServiceBoundaryTest` 扫描生产 SQL，禁止出现账号、门店、商品、订单等其他服务表名。
- AI 技能只读取本平台库和内部只读接口，不连接其他数据库。

## 6. 自动化测试与原始报告

本地执行：

```powershell
mvn -f services/pom.xml -pl engagement-platform-service -am `
  -Dmaven.repo.local=D:/maven-repository test
```

测试覆盖：

| 测试 | 覆盖内容 |
| --- | --- |
| `PlatformRemoteClientTest` | 成功、404、超时、5xx 单次重试、POST 不自动重试、真实 A 服务 JSON 字段与内部请求头 |
| `StationMessageClientTest` | 消息依赖失败不回滚主业务，并生成可查询的审计轨迹 |
| `ReviewOrderMarkCompensationTest` | 固定幂等标识下的补偿成功、失败记账和多副本抢占入口 |
| `PlatformInternalContractTest` | 健康检查、内部鉴权、审计幂等及 4 个内部接口 wire DTO |
| `FileStorageServiceTest` | 文件类型、大小、业务类型与路径穿越防护 |
| `EngagementPlatformRouteTest` | Gateway 的 D 路由集合及外部 `/internal/**` 拒绝 |
| `EngagementPlatformServiceApplicationTest` | Spring 启动、Flyway 空库迁移、18 表精确归属 |
| `PublicEndpointInventoryTest` | 标准要求的 59 个公开操作逐项比对 |
| `ServiceBoundaryTest` | 禁止跨服务数据库表访问 |

原始 XML/TXT 报告生成在 `services/engagement-platform-service/target/surefire-reports`。`.github/workflows/microservices-ci.yml` **设计为**在 MySQL 8.4 的 `aituan_platform` 空库上运行全部测试、打包 JAR、上传报告并验证 Docker 镜像；是否实际通过必须以对应分支的 GitHub Actions 成功记录和原始日志为准。

## 7. 构建和运行

本地使用 H2 快速启动（只用于开发）：

```powershell
mvn -f services/pom.xml -pl engagement-platform-service -am `
  -Dmaven.repo.local=D:/maven-repository spring-boot:run
```

连接 MySQL 时至少设置：

```text
AITUAN_PLATFORM_DATASOURCE_URL=jdbc:mysql://localhost:3306/aituan_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
AITUAN_PLATFORM_DATASOURCE_USERNAME=aituan_platform_svc
AITUAN_PLATFORM_DATASOURCE_PASSWORD=<secret>
AITUAN_PLATFORM_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver
AITUAN_IDENTITY_BASE_URL=http://localhost:8081
AITUAN_MERCHANT_BASE_URL=http://localhost:8082
AITUAN_TRADE_BASE_URL=http://localhost:8083
AITUAN_INTERNAL_SERVICE_TOKEN=<shared-secret>
AITUAN_JWT_SECRET=<shared-jwt-secret>
```

启动后验证：

```powershell
Invoke-RestMethod http://localhost:8084/actuator/health
Invoke-RestMethod http://localhost:8084/actuator/info
```

## 8. Docker、Kubernetes 与 CI/CD

- Dockerfile：`deploy/microservices/engagement-platform-service/Dockerfile`，多阶段构建、非 root 用户、容器端口 8084。
- K8s：`k8s/13-engagement-platform-service.yaml`，包含 PVC、Service、Deployment、readiness/liveness、资源限制和 Secret 引用。
- Secret 字段示例：`k8s/secret-engagement-platform.example.yaml`，真实值不得提交。
- CI 定义：`.github/workflows/microservices-ci.yml`；本地文件存在不等于流水线已经执行成功。
- 发布定义：`.github/workflows/microservices-deploy.yml`，计划在测试通过后推送 `sha-xxxxxxx` 镜像；选择部署时等待 rollout、执行集群内健康检查，失败上传资源、Deployment 和日志诊断。实际交付需附 Actions、镜像和 rollout 运行证据。

K8s 部署前必须先创建 `aituan_platform` 及账号，并创建 `engagement-platform-secret`。Gateway 已增加成员 D 的精确路径集合；本地默认转发到 `http://localhost:8084`，部署时通过 `AITUAN_ENGAGEMENT_BASE_URL=http://engagement-platform-service:8084` 覆盖。

## 9. 与其他成员联调契约

联调时应优先检查以下事项：

1. A 服务实现用户摘要、站内消息、成长值和用户指标接口，并接受 D 的内部调用头。
2. B 服务实现门店/商品快照、商家账号映射和商家指标接口。
3. C 服务实现订单快照、评价资格、幂等订单已评价命令和订单指标接口。
4. A/B/C 调用 D 的评分摘要、互动指标、治理指标或审计接口时，使用统一内部令牌和调用方名称。
5. 合并其他成员路由时保留本分支 Gateway 的成员 D 精确路径，不要改成笼统的 `/api/admin/**`。

本分支没有提交或推送 Git；合并前仍需按共同文件负责人规则解决 Gateway 配置的并行修改。
