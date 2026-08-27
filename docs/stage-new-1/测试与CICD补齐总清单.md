# 测试与 CI/CD 补齐总清单

> 审查日期：2026-08-26
> 审查范围：现有自动化测试、端到端业务场景自动化、GitHub Actions、Docker Compose CI/CD、新服务器部署文档和 Kubernetes 缺口。

## 1. 结论

当前项目已经具备一定自动化测试和 Docker 镜像化 CI/CD 基础，但距离课程截图中的“测试补齐并跑通”和“容器化 CI/CD 部署到 Kubernetes”还有差距。

核心判断如下：

| 项目 | 当前状态 | 结论 |
| --- | --- | --- |
| 单元测试 | 后端、Flutter、Web API 层均有自动化测试 | 基本具备，但缺覆盖率统计和阈值 |
| 集成 / API 测试 | 后端有 Spring Boot + MockMvc + H2 MySQL mode 测试 | 部分具备，缺真实 MySQL service container 迁移测试 |
| 端到端业务场景自动化 | 未发现覆盖完整业务闭环的 E2E 工程 | 不满足，全部业务场景尚未自动化覆盖 |
| 端到端测试 | 未发现 `integration_test`、Playwright、Cypress、Selenium 等 E2E 工程 | 缺失 |
| CI 测试门禁 | `aituan-ci` 覆盖后端、Web、Flutter、静态回归 | 基本具备 |
| CD 镜像部署 | `aituan-deploy` 能构建 backend/web 镜像并用 Docker Compose 部署 | 基本具备 |
| Kubernetes 部署 | 已新增 `k8s/` 部署文件，`aituan-deploy` 支持 `deploy_target=k8s` 并执行 `kubectl apply/rollout` | 基本具备，需配置真实集群 Secrets 后验收 |
| 新服务器配置 | 文档和本地脚本已切到 `8.220.192.106`，服务器当前健康 | 需要确认 GitHub Secrets / Variables 已同步 |

一句话总结：**现在有单元 / API 层自动化测试、覆盖率报告、Docker Compose 回退链路和 Kubernetes 版 CI/CD 部署链路；端到端业务场景自动化、真实 MySQL 集成验证仍需继续补齐。**

## 2. 当前已有测试

### 2.1 后端测试

当前后端测试目录：

```text
services/backend/src/test/java
```

盘点结果：

| 指标 | 数量 |
| --- | --- |
| 测试文件 | 15 个 |
| `@Test` 用例 | 81 个 |

主要覆盖：

| 测试文件 | 覆盖内容 |
| --- | --- |
| `AuthApiIntegrationTest.java` | 用户、商家、管理员登录，错误密码，token check |
| `SecurityBoundaryApiTest.java` | USER / MERCHANT / ADMIN 权限边界 |
| `CouponApiIntegrationTest.java` | 我的券、可领券、订单可用券、未登录领取拒绝 |
| `ComplaintApiIntegrationTest.java` | 用户投诉列表、详情、补充、商家 / 后台边界 |
| `TradeApiIntegrationTest.java` | 用户订单、支付参数、商家 / 后台订单入口边界 |
| `CommonFileControllerTest.java` | 文件上传、读取、非法业务类型 |
| `SupportServiceTest.java` | 客服会话、自动回复、平台介入、客服接管 |
| `InteractionServiceTest.java` | 评价、举报、审核、点赞、防重复 |
| `DiscoveryServiceTest.java` | 首页发现、筛选、排序、推荐 |
| `MessageApiIntegrationTest.java` | 站内消息 API |
| `AiAgentServiceTest.java` | AI 助手服务降级或接口行为 |

后端测试特点：

- 使用 `@SpringBootTest` 启动 Spring 容器。
- 使用 `MockMvc` 做 API 集成测试。
- 使用 `application-test.yml` 中的 H2 内存库，开启 MySQL mode。
- Maven 会把根目录 `database/migrations` 和 `database/seeds` 复制到 `db/migration`、`db/seed`，供 Flyway 测试环境执行。

### 2.2 Flutter 用户端测试

当前 Flutter 测试目录：

```text
apps/user_app/test
```

盘点结果：

| 指标 | 数量 |
| --- | --- |
| 测试文件 | 13 个 |
| `test/group` 条目 | 54 个 |

主要覆盖：

| 测试文件 | 覆盖内容 |
| --- | --- |
| `widget_test.dart` | APP 启动页基础渲染 |
| `web_unsupported_page_test.dart` | Web 不支持页、APK 下载入口 |
| `app_build_info_test.dart` | 构建信息 |
| `app_bottom_action_bar_test.dart` | 底部操作栏布局回归 |
| `core/utils/validator_test.dart` | 手机号、邮箱、账号校验 |
| `core/network/json_codec_test.dart` | JSON 弱类型解析 |
| `shared/enums/business_type_test.dart` | 业务类型、订单类型、订单状态映射 |
| `member_coupon_models_test.dart` | 会员、优惠券模型解析 |
| `features/coupon/coupon_repository_test.dart` | 优惠券 Repository 请求路径 |
| `features/support/support_repository_models_test.dart` | 客服模型和 Repository 请求路径 |
| `features/order/takeaway_fulfillment_text_test.dart` | 外卖履约状态文案 |
| `features/merchant/takeaway_amount_utils_test.dart` | 起送金额、金额展示 |
| `features/message/message_repository_models_test.dart` | 消息模型解析 |

当前没有发现：

```text
apps/user_app/integration_test
```

因此 Flutter 真机或模拟器端到端自动化测试目前缺失。

### 2.3 商家端和后台端 Web 测试

当前 Web 端使用 Vitest。

| 端 | 测试文件 | 覆盖重点 |
| --- | --- | --- |
| 商家端 | `apps/merchant_web/src/api.test.ts` | 登录、Token、请求头、错误处理、FormData、资源地址 |
| 后台端 | `apps/admin_web/src/api.test.ts` | 登录、Token、请求头、错误处理、编码、资源地址 |

盘点结果：

| 端 | 测试文件数 | `describe/it/test` 条目 |
| --- | --- | --- |
| 商家端 | 1 个 | 6 个 |
| 后台端 | 1 个 | 6 个 |

当前 Web 测试主要覆盖 API 封装层，尚未覆盖页面级交互，例如订单履约页、投诉页、客服页、评价审核页的组件行为。

### 2.4 静态回归脚本

当前有：

```text
scripts/verify/member_e_regression_checks.sh
```

该脚本使用 grep 检查评价、客服、投诉、平台客服、上传、审计等关键入口和文案是否被误删。它适合作为低成本回归门禁，但不能替代业务场景测试，因为它只验证代码文本结构，不真实运行用户流程。

### 2.5 端到端业务场景自动化状态

当前未发现可在 CI 中自动执行的端到端业务场景测试工程，例如：

```text
apps/user_app/integration_test
tests/e2e
playwright.config.*
cypress.config.*
```

`docs/爱团测试报告.md` 只能作为历史测试材料和用例来源，不能算作当前自动化测试已经达标。后续需要把其中的业务用例落成可执行脚本，并接入 GitHub Actions。

当前端到端自动化覆盖结论：

| 用例编号 | stage-new-1 业务场景 | 自动化 E2E 状态 | 结论 |
| --- | --- | --- | --- |
| UC01 | 用户注册、登录并进入受保护业务 | 有后端 API / 权限测试，缺从登录入口到受保护页面 / 接口的 E2E | 未完整满足 |
| UC02 | 用户搜索筛选商家/商品并查看详情 | 有后端发现服务测试，缺首页、筛选、详情页 E2E | 未完整满足 |
| UC03 | 用户维护个人资料、地址和收藏 | 缺资料、地址、收藏维护 E2E | 不满足 |
| UC04 | 用户完成外卖点单、优惠结算和模拟支付，并查看订单详情 | 有部分交易 API 测试，缺用户端完整点单支付 E2E | 未完整满足 |
| UC05 | 商家处理外卖订单并推进配送履约，用户接收订单状态消息 | 有部分交易 / 消息测试，缺商家端到用户端跨端 E2E | 未完整满足 |
| UC06 | 用户购买非外卖服务并获得券码凭证 | 缺非外卖购买、支付、出券 E2E | 不满足 |
| UC07 | 用户预约到店/团购服务并由商家核销完成服务 | 缺预约生成、商家确认、券码核销 E2E | 不满足 |
| UC08 | 用户申请取消与退款，商家或平台处理退款 | 缺取消退款、资源释放、商家 / 平台处理 E2E | 不满足 |
| UC09 | 用户完成订单后发布评价，商家回复，平台审核或治理评价 | 有后端评价服务测试，缺用户端、商家端、后台端跨端 E2E | 未完整满足 |
| UC10 | 用户提交投诉，商家或平台受理、处理并关闭 | 有后端投诉 API 测试，缺完整工单状态 E2E | 未完整满足 |
| UC11 | 用户联系客服，AI、商家或平台客服进行回复 | 有后端客服 / AI 测试，缺用户、商家、平台客服跨端 E2E | 未完整满足 |
| UC12 | 用户查看会员成长值、领取并在下单时使用优惠券 | 有会员 / 优惠券模型和 API 测试，缺领取到下单使用 E2E | 未完整满足 |
| UC13 | 商家维护门店资料、履约规则和商品/服务目录，并管理上下架状态 | 缺商家资料、规则、商品服务维护 E2E | 不满足 |

因此，**端到端测试覆盖清单中的全部业务场景目前不满足**。

## 3. 当前 CI/CD 状态

### 3.1 `aituan-ci`

文件：

```text
.github/workflows/ci.yml
```

触发：

- `pull_request`
- `workflow_dispatch`

当前包含：

| Job | 内容 |
| --- | --- |
| `static-regression` | 执行 `scripts/verify/member_e_regression_checks.sh` |
| `backend` | Java 17 + Maven test |
| `web` | merchant/admin matrix，`npm test` 后 `npm run build` |
| `flutter` | Flutter pub get、analyze、test、web build |

评价：

- PR 门禁基础完整。
- 任一 job 失败会阻断该 workflow。
- 适合证明“测试失败后流水线停下”。

### 3.2 `aituan-deploy`

文件：

```text
.github/workflows/deploy.yml
```

触发：

- push 到 `main`
- 手动 `workflow_dispatch`

当前包含：

| 阶段 | 内容 |
| --- | --- |
| 变量校验 | 校验 `SERVER_ORIGIN` 必须是站点 origin，不能带 `/api` |
| 后端测试和打包 | `mvn test`，再 package JAR |
| Web 构建 | merchant/admin Vite 构建 |
| Flutter 测试和构建 | `flutter analyze`、`flutter test`、`flutter build web` |
| 镜像构建 | 构建 backend 和 web 镜像 |
| 镜像推送 | 推送到 GHCR，标签含 `sha-短提交号` 和 `main` |
| 部署 | SSH 到服务器，执行 Docker Compose pull / up |
| 健康检查 | `curl http://127.0.0.1/actuator/health` |

评价：

- 满足“取代码、安装依赖、编译、测试、制作镜像、健康检查”的主要链路。
- 镜像有版本号，不只使用 `latest`。
- 当前部署方式是 Docker Compose，不是 Kubernetes。

需要注意：

- `deploy.yml` 里 Web 阶段目前只构建 merchant/admin，没有显式运行 `npm test`。PR 的 `ci.yml` 会跑 Web 测试，但部署 workflow 自己没有跑 Web Vitest。为了满足“push 后流水线自动完成测试”，建议在 `deploy.yml` 中也补上 `npm test`。

### 3.3 `aituan-android-apk`

文件：

```text
.github/workflows/android-apk.yml
```

触发：

- 手动 `workflow_dispatch`

当前包含：

- 校验 API origin；
- `flutter analyze`；
- `flutter test`；
- `flutter build apk --debug`；
- 上传 GitHub Actions Artifact；
- 可选上传到服务器 downloads 目录。

评价：

- APK 自动打包链路存在。
- 但它是手动触发，不属于每次 main push 的自动部署链路。

## 4. 新服务器与部署状态

当前新服务器文档：

```text
docs/stage-new-1/新服务器Docker部署与CICD说明.md
```

记录的新服务器信息：

| 项目 | 值 |
| --- | --- |
| SSH 别名 | `aituan-new` |
| 服务器 IP | `8.220.192.106` |
| 部署目录 | `/opt/aituan/app` |
| 数据目录 | `/opt/aituan/data` |
| 备份目录 | `/opt/aituan/backups` |
| 当前访问方式 | HTTPS/域名（`https://aituan.2b.gs`），SSH/部署使用 IP |

已核对的新服务器状态：

- `deploy/docker-compose.server.yml` 存在；
- `deploy/docker-compose.cicd.yml` 存在；
- `deploy/.env` 存在；
- `.config` 存在；
- `aituan-mysql`、`aituan-backend`、`aituan-nginx` 正在运行；
- `curl http://127.0.0.1/actuator/health` 返回 `{"status":"UP"}`。
- 2026-08-26 已完成域名 HTTPS 切换，`https://aituan.2b.gs/`、`/web/`、`/merchant/`、`/admin/`、`/downloads/aituan-user-server-debug.apk`、`/actuator/health` 均返回 200。

当前实际运行形态：

```text
Docker Compose 本地构建版
```

不是：

```text
Kubernetes 部署
```

也不是完全由 GitHub Actions 部署出来的 CI/CD 镜像版。

需要在 GitHub 仓库确认：

| 配置 | 应为 |
| --- | --- |
| `SERVER_ORIGIN` | `https://aituan.2b.gs` |
| `SERVER_HOST` | `8.220.192.106` |
| `SERVER_PORT` | `22` |
| `SERVER_USER` | 当前部署用户 |
| `SERVER_APP_DIR` | `/opt/aituan/app` |
| `SERVER_KNOWN_HOSTS` | `ssh-keyscan -H 8.220.192.106` 输出 |
| `AUTO_DEPLOY_PRODUCTION` | 如需 push main 自动部署，设为 `true` |

## 5. 对照课程要求的缺口清单

### 5.1 测试要求缺口

| 要求 | 当前情况 | 缺口 | 建议补齐方式 |
| --- | --- | --- | --- |
| 单元测试关键类、方法、业务规则和异常分支 | 后端、Flutter、Web API 层已有 | 缺统一覆盖率报告 | 加 JaCoCo、Flutter coverage、Vitest coverage |
| 集成 / API 测试覆盖模块调用、数据库访问和对外接口 | 后端 MockMvc + H2 已有 | 缺真实 MySQL 8 集成验证；外部接口多为配置或降级，没有统一 mock contract | GitHub Actions 增加 MySQL service container；对上传、地图、AI 增加 mock/降级测试说明 |
| 端到端测试覆盖完整业务流程 | 当前未发现可执行 E2E 工程 | 自动化 E2E 缺失，业务场景没有全部覆盖 | 增加 Playwright/API E2E 和 Flutter `integration_test` |
| 测试失败时流水线停下 | CI 基本满足 | deploy workflow 缺 Web `npm test`，仍可能只构建不测 Web API 层 | 在 `deploy.yml` merchant/admin build 前补 `npm test` |
| 测试报告写清测试总数、通过数、失败数、失败原因和环境 | 现有报告可作为历史材料 | 缺最新 GitHub Actions 原始日志编号、覆盖率数字和 2026-08-26 新服务器 / K8s 状态 | 新增自动化测试执行记录 |
| 覆盖率数字可以提交但不能替代业务场景测试 | 当前无覆盖率数字 | 缺覆盖率生成和汇总 | 先生成报告，不设硬阈值；稳定后设最低阈值 |

### 5.2 业务场景自动化缺口

要求口径：**端到端测试要覆盖清单中的全部业务场景**。最终汇报可以重点讲至少 3 个代表性用例，但工程里不能只补 3 条后就认为全部满足。

| 用例编号 | stage-new-1 业务场景 | 当前自动化证据 | 自动化缺口 | 建议实现 |
| --- | --- | --- | --- | --- |
| UC01 | 用户注册、登录并进入受保护业务 | `AuthApiIntegrationTest`、`SecurityBoundaryApiTest` | 缺从登录入口到受保护业务入口的 E2E | 三类账号登录、未登录拦截、角色越权断言 |
| UC02 | 用户搜索筛选商家/商品并查看详情 | `DiscoveryServiceTest` | 缺首页、筛选、排序、详情页 E2E | 首页进入模块、搜索关键词、打开商家和商品详情 |
| UC03 | 用户维护个人资料、地址和收藏 | Flutter 有部分模型 / 页面代码，后端账号模块存在 | 缺资料编辑、地址增删改、收藏 / 取消收藏 E2E | 用户登录后维护资料、地址、收藏，并断言再次查询结果 |
| UC04 | 用户完成外卖点单、优惠结算和模拟支付，并查看订单详情 | `TradeApiIntegrationTest` 覆盖部分交易 API | 缺完整用户点单、试算、优惠、支付、详情 E2E | 用户进入外卖商家、加购、选券、下单、模拟支付、查看详情 |
| UC05 | 商家处理外卖订单并推进配送履约，用户接收订单状态消息 | `TradeApiIntegrationTest`、`MessageApiIntegrationTest` 覆盖部分行为 | 缺商家端处理与用户消息状态跨端 E2E | 商家接单、推进备餐 / 配送，用户查看订单状态和消息 |
| UC06 | 用户购买非外卖服务并获得券码凭证 | 部分交易接口和页面存在 | 缺非外卖购买出券 E2E | 用户购买团购 / 服务商品、支付后查看券码详情 |
| UC07 | 用户预约到店/团购服务并由商家核销完成服务 | 商家券码 / 预约页面存在 | 缺预约、确认、核销闭环 E2E | 用户预约，商家确认或核销，订单进入完成状态 |
| UC08 | 用户申请取消与退款，商家或平台处理退款 | 交易模块支持退款状态 | 缺退款状态和优惠券 / 券码 / 预约资源处理 E2E | 用户发起退款，商家或后台处理，断言订单和关联资源状态 |
| UC09 | 用户完成订单后发布评价，商家回复，平台审核或治理评价 | `InteractionServiceTest` | 缺跨用户端、商家端、后台端评价治理 E2E | 用户评价，商家回复，后台审核 / 屏蔽，用户端回看 |
| UC10 | 用户提交投诉，商家或平台受理、处理并关闭 | `ComplaintApiIntegrationTest`、`ComplaintServiceTest` | 缺投诉工单完整状态 E2E | 用户提交投诉，商家查看，后台受理 / 处理 / 关闭，用户查看状态 |
| UC11 | 用户联系客服，AI、商家或平台客服进行回复 | `SupportServiceTest`、`AiAgentServiceTest` | 缺客服会话、AI 降级、平台客服跨端 E2E | 用户发起客服，AI 回复或降级，商家 / 平台回复，关闭会话 |
| UC12 | 用户查看会员成长值、领取并在下单时使用优惠券 | `CouponApiIntegrationTest`、Flutter 优惠券测试 | 缺会员成长和优惠券使用 E2E | 用户查会员、领券、下单选券、支付后查看券状态 |
| UC13 | 商家维护门店资料、履约规则和商品/服务目录，并管理上下架状态 | 商家端 API 层测试只覆盖通用请求封装 | 缺商家资料、配送规则、商品服务维护 E2E | 商家编辑门店、设置履约规则、新增商品、上下架并在用户端验证展示 |

### 5.3 CI/CD 和 Kubernetes 缺口

| 要求 | 当前情况 | 缺口 | 建议补齐方式 |
| --- | --- | --- | --- |
| 前端、后端、数据库分别运行在容器中 | Docker Compose 已满足 | K8s 未满足 | 新增 `k8s/` manifests |
| 数据库官方镜像 | `mysql:8.0` 已满足 | K8s 中还没有 MySQL StatefulSet | 新增 MySQL `StatefulSet` 和 `Service` |
| 前后端提交 Dockerfile | 已有 `deploy/backend/Dockerfile`、`deploy/web/Dockerfile` | 无 | 保留 |
| push 后自动测试、构建镜像 | `deploy.yml` 基本具备 | 缺 Web `npm test`、MySQL service container 测试 | 修改 `deploy.yml` |
| 部署到 Kubernetes | 已新增 `k8s/` manifests，`aituan-deploy` 支持 `deploy_target=k8s` | 真实集群需配置 `KUBE_CONFIG`、K8s Secrets 和 Ingress/TLS | 在课程或测试集群运行 K8s 部署验收 |
| 成功和失败记录保留 | GitHub Actions 会保留日志，K8s job 失败时输出 `kubectl describe/logs` | 可继续补部署摘要 artifact | 上传测试报告、部署摘要和 kubectl 输出 |
| 镜像有版本号，不只用 latest | 已使用 `sha-短提交号` 和 `main`，K8s 通过 `kubectl set image` 使用 sha tag | 无 | 保留 sha tag 部署规则 |
| Kubernetes 部署文件、测试脚本、部署脚本提交 | 已新增 `k8s/` 和说明文档 | 真实集群 Secrets 不入仓库 | 按 `k8s/README.md` 准备集群配置 |

## 6. 建议新增文件清单

如果后续开始补齐，建议最小新增这些文件：

```text
k8s/namespace.yaml
k8s/mysql.yaml
k8s/backend.yaml
k8s/web.yaml
k8s/ingress-or-nodeport.yaml
k8s/README.md
docs/stage-new-1/Kubernetes部署与CI-CD补齐说明.md
docs/stage-new-1/测试执行记录-2026-08-26.md
```

如果要补自动化业务场景测试，建议新增：

```text
tests/e2e/api/business-flow.test.*
tests/e2e/README.md
```

或者如果选择浏览器 E2E：

```text
tests/e2e/playwright.config.*
tests/e2e/specs/order-flow.spec.*
tests/e2e/specs/voucher-flow.spec.*
tests/e2e/specs/complaint-flow.spec.*
```

Flutter 真机自动化则建议新增：

```text
apps/user_app/integration_test/
```

## 7. 建议实施顺序

### 第一步：补部署 workflow 测试完整性

目标：先让 `deploy.yml` 自己也完整跑测试。

建议改动：

- 在 `Build merchant web` 前增加 `npm test --prefix apps/merchant_web`；
- 在 `Build admin web` 前增加 `npm test --prefix apps/admin_web`；
- 增加 MySQL 8 service container，验证后端迁移至少能在真实 MySQL 上跑通。

验收：

- `aituan-deploy` 中任一测试失败时，不构建或不部署镜像。

### 第二步：补 Kubernetes 文件，但不立即切生产流量

目标：先让 K8s manifests 可以在 k3d / k3s / kind 中跑起来。

建议：

- 新增 namespace；
- MySQL 用 StatefulSet；
- backend/web 用 Deployment；
- web Service 暴露 HTTP；
- backend 使用 readiness/liveness 探针；
- Secret 和 ConfigMap 使用模板，不提交真实密钥。

验收：

- `kubectl apply -f k8s/` 成功；
- `kubectl rollout status deployment/aituan-backend` 成功；
- 集群内或测试端口访问 `/actuator/health` 成功。

### 第三步：新增 GitHub Actions K8s 部署路径

目标：满足“push 后自动完成 Kubernetes 部署和健康检查”。

建议：

- 保留 Docker Compose workflow 作为回退；
- 新增 deploy input，例如 `deploy_target: compose/k8s`；
- K8s 路径执行 `kubectl apply`、`kubectl set image`、`kubectl rollout status`；
- 失败时输出 `kubectl describe` 和 `kubectl logs`。

验收：

- GitHub Actions 日志能看到镜像 tag、rollout 结果和健康检查结果。

### 第四步：补齐全部业务场景自动化 E2E

目标：满足“端到端测试覆盖清单中的全部业务场景”。最终汇报可以从中挑 3 个代表性用例重点讲，但自动化工程需要覆盖全部清单。

建议按 stage-new-1 的 13 个 UC 全部落地：

1. `UC01` 用户注册、登录并进入受保护业务；
2. `UC02` 用户搜索筛选商家/商品并查看详情；
3. `UC03` 用户维护个人资料、地址和收藏；
4. `UC04` 用户完成外卖点单、优惠结算和模拟支付，并查看订单详情；
5. `UC05` 商家处理外卖订单并推进配送履约，用户接收订单状态消息；
6. `UC06` 用户购买非外卖服务并获得券码凭证；
7. `UC07` 用户预约到店/团购服务并由商家核销完成服务；
8. `UC08` 用户申请取消与退款，商家或平台处理退款；
9. `UC09` 用户完成订单后发布评价，商家回复，平台审核或治理评价；
10. `UC10` 用户提交投诉，商家或平台受理、处理并关闭；
11. `UC11` 用户联系客服，AI、商家或平台客服进行回复；
12. `UC12` 用户查看会员成长值、领取并在下单时使用优惠券；
13. `UC13` 商家维护门店资料、履约规则和商品/服务目录，并管理上下架状态。

验收：

- 每条用例有断言，不只是程序不报错；
- CI 中能独立运行；
- 失败会阻断部署。

### 第五步：补测试报告和覆盖率

目标：让材料能支撑评分。

建议：

- 后端加 JaCoCo 报告；
- Flutter 执行 `flutter test --coverage`；
- Vitest 执行 coverage；
- GitHub Actions 上传测试报告和覆盖率 artifact；
- `docs/爱团测试报告.md` 或新增记录中写明最新测试总数、通过数、失败数、运行环境。

验收：

- 报告里能对应到代码、测试和 CI 日志；
- 覆盖率数字只是辅助材料，不替代业务场景 E2E。

## 8. 优先级清单

| 优先级 | 任务 | 原因 |
| --- | --- | --- |
| P0 | 在 `deploy.yml` 补 Web `npm test` | 低成本，直接补齐 push 部署链路测试 |
| P0 | 新增 K8s manifests | 当前明确缺 Kubernetes |
| P0 | 新增 K8s 部署 job 或 workflow | 满足 CI/CD 部署到 Kubernetes 的评分点 |
| P1 | 增加真实 MySQL 8 集成测试 | 比 H2 更贴近生产 |
| P1 | 自动化全部业务场景 E2E | 满足业务场景测试要求 |
| P1 | 更新测试报告执行记录 | 材料与现状一致 |
| P2 | 覆盖率报告 | 支撑测试质量，但不能替代业务场景 |
| P2 | 部署日志 artifact 和回滚说明 | 提高可审计性 |

## 9. 当前可用于答辩的说法

可以如实说明：

1. 项目已有后端、Flutter、Web API 层自动化测试，并已接入 GitHub Actions。
2. 后端自动化测试覆盖认证、权限、交易、优惠券、投诉、客服、评价、文件上传等核心模块。
3. 当前不足是端到端业务场景尚未全部自动化，后续需要用 Playwright、API E2E 或 Flutter `integration_test` 覆盖全部业务场景清单。
4. 最终汇报可以从 `UC04` 外卖点单支付、`UC07` 预约核销、`UC10` 投诉处理等 13 个 E2E 用例中挑 3 条代表链路重点说明。
5. CI/CD 当前已能构建 GHCR 镜像，支持 Docker Compose 回退，并新增 Kubernetes 部署文件和 `kubectl rollout` 链路。
6. 新服务器已经完成 Docker Compose 部署并通过健康检查；K8s 链路作为课程标准部署路径，需在真实集群配置 `KUBE_CONFIG`、Secret 和 Ingress/TLS 后验收。

## 10. 五人分工建议

你当前拆的 4 块方向是对的，但团队有 5 个人，建议不要简单按“前 7 个 UC / 后 6 个 UC”切分。原因是 `UC04`、`UC05`、`UC08`、`UC09`、`UC10`、`UC11` 都是跨端或状态流转较重的用例，只按数量切会导致 E2E 负责人的压力过大。

### 10.1 除四块任务外还需要补的事项

| 事项 | 为什么必须有人负责 | 建议归属 |
| --- | --- | --- |
| 测试数据初始化与隔离 | E2E 依赖固定账号、商家、商品、优惠券、订单、券码、投诉等数据；不固定数据会导致 CI 不稳定 | 归入测试基础负责人 |
| 自动化测试报告归档 | 课程要求测试总数、通过数、失败数、失败原因和环境；不能只放截图 | 归入 CI/CD 负责人，文档负责人复核 |
| UC01-UC13 追踪表 | 需要把业务场景对应到需求、页面、接口、代码、测试、CI job | 归入文档整合负责人 |
| E2E 执行说明 | 需要说明本地和 CI 怎么跑、依赖什么环境、失败怎么定位 | 归入 E2E 负责人，文档负责人统一 |
| K8s 部署与回滚说明 | 除 YAML 外，还要有查看 Pod、日志、rollout、回滚的命令 | 归入 CI/CD 负责人 |
| Part 2 预研材料 | `stage-new-1/软工小学期.md` 还有微服务、自动扩缩容、故障处理、性能测试 | 归入文档整合负责人，技术负责人补输入 |

### 10.2 推荐 5 人分工

| 成员 | 主要负责 | 对应清单 | 交付物 |
| --- | --- | --- | --- |
| 成员 A | 单元测试、集成 / API 测试、覆盖率、测试数据准备 | `T01-T04`，测试数据初始化 | 补测试缺口、覆盖率配置、MySQL 集成验证、测试数据说明 |
| 成员 B | GitHub Actions、新服务器配置、K8s 部署、CI 测试报告 artifact | `C01-C05`、`D01` | 修改 workflow、补 K8s manifests、上传测试报告、写部署和回滚说明 |
| 成员 C | 前 4 个端到端业务场景 | `UC01-UC04` | 注册登录、搜索详情、资料地址收藏、外卖点单支付 E2E |
| 成员 D | 中间 5 个端到端业务场景 | `UC05-UC09` | 外卖履约消息、非外卖出券、预约核销、退款、评价治理 E2E |
| 成员 E | 后 4 个端到端业务场景，测试报告和追踪表整合 | `UC10-UC13`，UC 追踪表，最终测试执行记录 | 投诉、客服 AI、会员优惠券、商家资料商品维护 E2E，整合最终文档 |

这个分法的好处：

1. `UC04` 和 `UC05` 没有落到同一个人，外卖交易和商家履约压力拆开。
2. `UC08-UC11` 这些状态机和跨端治理场景被分散到 D、E 两人。
3. 第 5 人不是只做文档，而是同时负责 `UC10-UC13` 和最终材料闭环，避免材料和代码对不上。
4. 测试基础和 CI/CD 各有专人，E2E 负责人不用同时处理环境、报告、K8s。

### 10.3 如果沿用原 4 块分工的调整建议

如果必须保留你原来的 4 块，可以改成：

| 原任务 | 调整建议 |
| --- | --- |
| 负责 `T01-T04` | 继续保留，但增加“测试数据初始化与覆盖率报告” |
| 负责 `C01-C05`、`D01` | 继续保留，但增加“K8s 部署说明、回滚说明、CI artifact 汇总” |
| 负责 `UC01-UC07` | 建议拆出去 `UC05` 或 `UC07`，否则和 `UC04` 连续重场景太多 |
| 负责 `UC08-UC13` | 建议保留 `UC08-UC11`，把 `UC12-UC13` 给第 5 人 |
| 第 5 人 | 接 `UC12-UC13`，并负责 UC01-UC13 追踪表、测试报告整合、Part 2 预研 |

## 11. 集中缺口总表

| 编号 | 类别 | 要求 | 当前自动化证据 | 是否满足 | 缺什么 | 建议新增 / 修改 |
| --- | --- | --- | --- | --- | --- | --- |
| UC01 | 端到端业务场景 | 用户注册、登录并进入受保护业务 | 后端 `AuthApiIntegrationTest`、`SecurityBoundaryApiTest` | 未完整满足 | 缺从前端入口或统一 E2E 入口执行 | `tests/e2e/specs/uc01-auth-flow.spec.*` |
| UC02 | 端到端业务场景 | 用户搜索筛选商家/商品并查看详情 | 后端 `DiscoveryServiceTest` | 未完整满足 | 缺首页、筛选、排序、详情页 E2E | `tests/e2e/specs/uc02-discovery-flow.spec.*` |
| UC03 | 端到端业务场景 | 用户维护个人资料、地址和收藏 | 部分 Flutter 页面 / 模型代码存在 | 不满足 | 缺资料、地址、收藏维护 E2E | `tests/e2e/specs/uc03-account-assets-flow.spec.*` |
| UC04 | 端到端业务场景 | 用户完成外卖点单、优惠结算和模拟支付，并查看订单详情 | 后端 `TradeApiIntegrationTest` 覆盖部分 API | 未完整满足 | 缺完整用户端点单支付闭环 | `tests/e2e/specs/uc04-takeaway-order-flow.spec.*` |
| UC05 | 端到端业务场景 | 商家处理外卖订单并推进配送履约，用户接收订单状态消息 | 后端交易 / 消息测试覆盖部分行为 | 未完整满足 | 缺商家端到用户端跨端履约闭环 | `tests/e2e/specs/uc05-delivery-fulfillment-flow.spec.*` |
| UC06 | 端到端业务场景 | 用户购买非外卖服务并获得券码凭证 | 部分交易接口和页面存在 | 不满足 | 缺非外卖购买出券 E2E | `tests/e2e/specs/uc06-service-voucher-flow.spec.*` |
| UC07 | 端到端业务场景 | 用户预约到店/团购服务并由商家核销完成服务 | 商家券码 / 预约页面存在 | 不满足 | 缺预约、确认、核销闭环 E2E | `tests/e2e/specs/uc07-booking-redeem-flow.spec.*` |
| UC08 | 端到端业务场景 | 用户申请取消与退款，商家或平台处理退款 | 交易模块支持退款状态 | 不满足 | 缺退款状态和关联资源释放 E2E | `tests/e2e/specs/uc08-refund-flow.spec.*` |
| UC09 | 端到端业务场景 | 用户完成订单后发布评价，商家回复，平台审核或治理评价 | 后端 `InteractionServiceTest` | 未完整满足 | 缺跨端评价治理闭环 | `tests/e2e/specs/uc09-review-governance-flow.spec.*` |
| UC10 | 端到端业务场景 | 用户提交投诉，商家或平台受理、处理并关闭 | 后端 `ComplaintApiIntegrationTest`、`ComplaintServiceTest` | 未完整满足 | 缺完整投诉工单状态 E2E | `tests/e2e/specs/uc10-complaint-flow.spec.*` |
| UC11 | 端到端业务场景 | 用户联系客服，AI、商家或平台客服进行回复 | 后端 `SupportServiceTest`、`AiAgentServiceTest` | 未完整满足 | 缺客服、AI、商家、平台跨端 E2E | `tests/e2e/specs/uc11-support-ai-flow.spec.*` |
| UC12 | 端到端业务场景 | 用户查看会员成长值、领取并在下单时使用优惠券 | 后端 `CouponApiIntegrationTest`、Flutter 优惠券测试 | 未完整满足 | 缺会员成长和优惠券使用 E2E | `tests/e2e/specs/uc12-member-coupon-flow.spec.*` |
| UC13 | 端到端业务场景 | 商家维护门店资料、履约规则和商品/服务目录，并管理上下架状态 | 商家端 API 层测试只覆盖请求封装 | 不满足 | 缺商家资料、履约规则、商品服务维护 E2E | `tests/e2e/specs/uc13-merchant-catalog-flow.spec.*` |
| T01 | 单元测试 | 关键类、方法、业务规则、异常分支 | 后端、Flutter、Web API 层已有 | 部分满足 | 缺覆盖率报告和阈值 | JaCoCo、Flutter coverage、Vitest coverage |
| T02 | 集成 / API 测试 | 模块调用、数据库访问、外部接口 | 后端 MockMvc + H2 MySQL mode | 部分满足 | 缺真实 MySQL 8 service container；缺外部接口 mock contract | `.github/workflows/ci.yml` 增加 MySQL 服务和迁移验证 |
| T03 | Web 页面测试 | 商家端 / 后台端页面交互 | 仅有 `src/api.test.ts` | 不满足 | 缺页面级组件测试或浏览器 E2E | Vitest component tests 或 Playwright |
| T04 | Flutter 端到端 | APP 页面完整流程 | `apps/user_app/test` 有单测和 Widget 测试 | 不满足 | 缺 `integration_test` | `apps/user_app/integration_test/` |
| C01 | CI 测试门禁 | PR 自动测试失败即停 | `aituan-ci` 已覆盖后端、Web、Flutter、静态回归，并上传后端 Surefire/JaCoCo、Web Vitest、Flutter coverage artifact | 满足 | 后续可继续补覆盖率阈值 | 现已上传测试报告和覆盖率 artifact |
| C02 | CD 测试门禁 | push 部署前完整测试 | `aituan-deploy` 跑后端、merchant/admin Web、Flutter 测试 | 满足 | 后续可继续补部署流水线报告归档 | 已在 `deploy.yml` 构建 Web 前补 `npm test` |
| C03 | Kubernetes 部署 | push 后部署到 K8s 并健康检查 | 已新增 `k8s/` manifests，`aituan-deploy` 支持 `deploy_target=k8s` 并执行 `kubectl rollout status` | 满足 | 真实集群需配置 `KUBE_CONFIG` 和 K8s Secrets | 配置 GitHub Secrets 后运行 K8s 部署验证 |
| C04 | 镜像版本 | 镜像不能只用 latest | Compose 与 K8s 均使用 `sha-短提交号`，`main` 仅作辅助标签 | 满足 | 无 | K8s 通过 `kubectl set image` 使用 `${IMAGE_TAG}` |
| C05 | 测试报告产物 | 测试总数、通过数、失败数、失败原因、环境 | 文档有历史材料 | 不满足当前自动化口径 | 缺最新 CI 原始报告 artifact | 上传 Surefire、Vitest、Flutter test、coverage artifact |
| D01 | 新服务器配置 | 新服务器变量、密钥、known_hosts | 文档写到 `8.220.192.106`，服务器健康 | 部分满足 | GitHub Secrets / Variables 需在网页端确认 | 核对 `SERVER_ORIGIN`、`SERVER_HOST`、`SERVER_KNOWN_HOSTS` |
