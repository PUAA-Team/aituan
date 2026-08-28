# 爱团本地生活服务综合平台

爱团是一个面向本地生活服务场景的综合平台，覆盖用户消费、商家经营、平台治理和 AI 辅助。项目围绕“生活助手平台”课程题目设计，实现了用户端 APP / Web、商家端 Web、后台端 Web、Spring Boot 后端、MySQL 数据库、自动化测试、Docker Compose 回退部署和 Kubernetes CI/CD 主部署链路。

项目目标是一套可以运行、可以联调、可以部署、可以测试、可以展示的课程级完整软件工程项目。

## 1. 项目概览

### 1.1 核心定位

- 面向用户：提供本地商家搜索、外卖点餐、团购预约、优惠券、会员、评价、客服、投诉和 AI 助手。
- 面向商家：提供门店资料、商品/服务管理、订单履约、券码核销、评价回复和客服会话。
- 面向平台：提供商户治理、商品治理、订单治理、会员优惠券配置、评价审核、投诉处理、平台客服、审计日志和系统配置。

### 1.2 八大服务模块

| 服务模块 | 用户侧能力 | 履约方式 |
| --- | --- | --- |
| 外卖 | 商家浏览、商品点单、购物车、地址选择、模拟支付、配送时间线 | 点单 + 配送模拟 |
| 团购 | 套餐浏览、购买、券码展示、到店使用 | 券码核销 |
| 酒店 | 房型/服务展示、预约信息、凭证展示 | 预约 / 凭证使用 |
| 休闲娱乐 | 项目、套餐、时段和到店须知展示 | 预约 / 券码 |
| 电影演出 | 场次、票档、入场规则展示 | 票券核销 |
| 丽人医美 | 项目流程、服务时长、注意事项展示 | 预约服务 |
| 景点门票 | 票种、入园日期、开放时间展示 | 电子票核销 |
| 洗脚按摩 | 项目时长、到店/上门说明、预约时间展示 | 预约 / 服务完成 |

### 1.3 当前交付形态

| 交付项 | 说明 |
| --- | --- |
| 用户端 APP | Flutter Android APK，支持用户主流程演示。 |
| 用户端 Web | Flutter Web 预览版，由 Nginx 托管在 `/web/`，用于移动宽度下的用户端 E2E 与演示。 |
| 商家端 Web | Vue 3 + TypeScript 商家经营控制台，由 Nginx 托管在 `/merchant/`。 |
| 后台端 Web | Vue 3 + TypeScript 平台管理后台，由 Nginx 托管在 `/admin/`。 |
| 后端服务 | Spring Boot 3 + Java 17，提供统一 REST API。 |
| 数据库 | MySQL 8，使用 Flyway 管理 V001-V019 迁移和 repeatable seed 数据。 |
| 部署 | Kubernetes/k3s 为当前课程主部署链路；Docker Compose 保留为单机/回退部署；常规 JAR + Nginx 可用于手动安装。 |
| 测试 | 后端 JUnit/MockMvc/JaCoCo、MySQL 迁移 smoke test、Flutter analyze/test/coverage、Vitest coverage、Bash 静态回归、Playwright UC01-UC13 E2E。 |

线上演示地址已部署，可访问：

```text
https://aituan.2b.gs
```

## 2. 技术栈

| 层级 | 技术 |
| --- | --- |
| 用户端 | Flutter、Dart、Android APK、Flutter Web |
| 商家端 / 后台端 | Vue 3、TypeScript、Vite、Vitest、happy-dom |
| 后端 | Java 17、Spring Boot 3、Spring Security、JWT、JdbcTemplate、Flyway |
| 数据库 | MySQL 8；测试环境使用 H2 `MODE=MySQL`；CI 使用真实 MySQL 做迁移 smoke test |
| 文件与资源 | 本地文件存储或外部图床配置，可通过 `.config` 切换 |
| AI 能力 | 后端 AI Assistant + Skills，支持外部模型调用和本地降级 |
| 部署 | Docker Compose、Nginx、GHCR、GitHub Actions、Kubernetes/k3s、kubectl、Let's Encrypt HTTPS |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、JaCoCo、flutter_test、Vitest、Playwright、Bash 静态回归 |

## 3. 目录结构

```text
.
├─ .github/
│  └─ workflows/                # CI、部署、Android APK 构建 workflow
├─ apps/
│  ├─ user_app/                 # Flutter 用户端 APP / Web
│  ├─ merchant_web/             # 商家端 Vue Web
│  └─ admin_web/                # 后台端 Vue Web
├─ services/
│  └─ backend/                  # Spring Boot 后端服务
├─ database/
│  ├─ migrations/               # Flyway 版本迁移脚本
│  └─ seeds/                    # Flyway repeatable 演示 seed
├─ deploy/                      # Docker Compose、Nginx、Dockerfile、部署示例配置
├─ k8s/                         # Kubernetes manifests 与部署说明
├─ scripts/
│  ├─ build/                    # 本地和服务器版构建脚本
│  ├─ dev/                      # 本地启动脚本
│  ├─ release/                  # 构建产物清理脚本
│  └─ verify/                   # 静态回归验证脚本
├─ tests/
│  └─ e2e/                      # Playwright UC01-UC13 端到端测试
├─ docs/                        # 需求、设计、测试、部署、阶段交付文档
└─ README.md                    # 项目总览
```

说明：`deploy/artifacts/`、测试报告、APK、压缩包等属于构建/部署产物，默认不提交到仓库。

## 4. 功能清单

### 4.1 用户端

- 登录注册：账号密码登录、邮箱验证码、找回密码、Token 校验。
- 首页发现：八大服务模块、推荐商品、附近商家、搜索入口。
- 搜索浏览：关键词搜索、模块筛选、商家详情、商品/服务详情。
- 外卖点单：购物车、起送价、配送费、地址选择、确认订单、模拟支付、履约状态。
- 非外卖服务：团购、门票、预约类服务的订单、券码、二维码、核销状态。
- 订单中心：外卖订单、服务订单、预约详情、券码详情、配送跟踪。
- 用户资产：个人资料、地址、收藏、消息、会员等级、成长值、优惠券。
- 互动售后：发布评价、我的评价、评价详情、客服会话、投诉提交与查看。
- AI 助手：基于订单、优惠券、投诉、评价和客服上下文提供智能回复与快捷入口。

### 4.2 商家端

- 商家登录和 Token 管理。
- 经营总览：订单、交易额、待处理评价、客服咨询等指标。
- 门店资料：门店信息、营业状态、图片上传。
- 商品/服务管理：外卖商品、非外卖服务、套餐与服务项目维护。
- 订单履约：外卖接单、备餐、出餐、配送推进。
- 券码核销：查询券码、核销券码、避免重复核销。
- 互动售后：评价管理、评价回复、客服会话处理。

### 4.3 后台端

- 管理员登录和后台权限边界。
- 平台总览和治理看板。
- 商户门店治理、商品服务治理。
- 用户管理、会员等级、优惠券模板配置。
- 订单治理、预约治理、券码治理。
- 评价审核、投诉工单、平台客服。
- 管理员资料、系统配置、审计日志。

### 4.4 后端与基础能力

- 统一 API 前缀：`/api/open`、`/api/app`、`/api/merchant`、`/api/admin`、`/api/common`。
- JWT 鉴权与 USER / MERCHANT / ADMIN 角色隔离。
- 统一响应结构和统一异常处理。
- Flyway 迁移和幂等 seed 数据。
- 文件上传和访问。
- 模拟支付、配送状态推进、券码核销。
- 会员成长值、优惠券领取、下单抵扣、退款释放。
- AI 助手配置、模型调用、Skill 编排和降级回复。

## 5. 配置说明

项目通过 `.config`、`deploy/.env`、GitHub Actions Variables / Secrets 与 Kubernetes Secret 区分业务配置、部署配置和敏感配置。真实密钥、数据库密码、JWT secret、AI key、邮箱授权码、图床 Token、kubeconfig、证书私钥不应提交到 Git。

### 5.1 `.config`

模板文件：

```text
.config.example
```

常见配置项：

```properties
aituan.security.jwt-secret=<LONG_RANDOM_SECRET>

aituan.ai.enabled=false
aituan.ai.api-url=<AI_API_BASE_URL>
aituan.ai.api-key=<AI_API_KEY>
aituan.ai.model=<MODEL_NAME>
aituan.ai.timeout-seconds=20
aituan.ai.max-tokens=800
aituan.ai.temperature=0.25

aituan.mail.enabled=false
aituan.mail.debug-return-code=false

aituan.upload.strategy=local
aituan.map.provider=local
```

说明：

- `aituan.security.jwt-secret` 公开部署时必须改为强随机字符串。
- `aituan.ai.enabled=false` 时 AI 助手会走本地降级回复。
- `aituan.mail.debug-return-code=false` 用于避免验证码直接返回到前端。
- 图片上传和地图服务均可通过 `.config` 切换为外部服务。

### 5.2 `deploy/.env`

模板文件：

```text
deploy/.env.example
```

常见配置项：

```dotenv
MYSQL_DATABASE=<DB_NAME>
MYSQL_USER=<DB_USER>
MYSQL_PASSWORD=<DB_PASSWORD>
MYSQL_ROOT_PASSWORD=<DB_ROOT_PASSWORD>
AITUAN_DATA_DIR=<DATA_DIR>
AITUAN_CONFIG_HOST_FILE=<CONFIG_FILE>

AITUAN_IMAGE_REGISTRY=<IMAGE_REGISTRY>/<OWNER>/<REPO>
AITUAN_IMAGE_TAG=sha-<SHORT_COMMIT>
AITUAN_DOWNLOADS_DIR=<DOWNLOADS_DIR>
AITUAN_NGINX_SERVER_NAME=<DOMAIN>
AITUAN_LETSENCRYPT_DIR=/etc/letsencrypt
AITUAN_CERTBOT_WEBROOT=/var/www/certbot
```

`deploy/.env` 只保存在部署机器，不提交到仓库。

### 5.3 GitHub Actions / Kubernetes 配置

自动化部署通常需要 Repository Variables：

```text
SERVER_ORIGIN=https://<DOMAIN>
AUTO_DEPLOY_PRODUCTION=true
DEPLOY_TARGET=k8s
K8S_NAMESPACE=aituan
```

Kubernetes 主部署链路需要 Production Secrets：

```text
KUBE_CONFIG=<Kubernetes 集群 kubeconfig 内容>
K8S_MYSQL_USER=<MySQL 用户名>
K8S_MYSQL_PASSWORD=<MySQL 密码>
K8S_MYSQL_ROOT_PASSWORD=<MySQL root 密码>
K8S_APP_CONFIG=<后端 .config 完整内容>
```

如果 GHCR 镜像保持私有，还需要：

```text
GHCR_PULL_USERNAME=<GitHub 用户名或机器人账号>
GHCR_PULL_TOKEN=<具备 read:packages 权限的 PAT>
```

这些值只放在 GitHub Secrets 或 Kubernetes Secret 中，不写入 README、脚本、YAML 或提交记录。

## 6. 快速启动与常用脚本

本仓库当前开发机以 Windows 11 / PowerShell 为主。后端 Maven 缓存、Flutter Pub 缓存、Gradle 缓存、运行日志和构建产物优先放在 D 盘，避免占用 C 盘空间。

### 6.1 后端 Demo 环境

Demo 环境适合本地开发和接口体验。默认会使用 H2 内存数据库，并自动执行 Flyway 迁移和演示数据。

推荐使用项目脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_backend.ps1
```

也可以手动构建和启动：

```powershell
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" test
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -DskipTests package
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

验证：

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui.html
```

### 6.2 MySQL 开发环境与迁移

MySQL 环境只需要先创建库和用户；表结构、索引和演示 seed 由后端启动时通过 Flyway 自动执行。

```sql
CREATE DATABASE <DB_NAME>
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER '<DB_USER>'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON <DB_NAME>.* TO '<DB_USER>'@'%';
FLUSH PRIVILEGES;
```

启动后端时配置环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:AITUAN_DATASOURCE_URL = "jdbc:mysql://<DB_HOST>:<DB_PORT>/<DB_NAME>?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:AITUAN_DATASOURCE_USERNAME = "<DB_USER>"
$env:AITUAN_DATASOURCE_PASSWORD = "<DB_PASSWORD>"
$env:AITUAN_CONFIG_FILE = "<CONFIG_FILE>"

java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

迁移口径：

- 迁移脚本目录：`database/migrations/`。
- 演示 seed 目录：`database/seeds/R__seed_demo_data.sql`。
- Maven 会把上述目录复制到后端 classpath 的 `db/migration` 和 `db/seed`。
- 当前最新迁移为 `V019__ai_assistant_conversation.sql`，后续新增迁移应从 `V020__...sql` 开始。
- Flyway 迁移脚本必须兼容 MySQL 8 和 H2 `MODE=MySQL` 空库迁移。
- CI 中 `MysqlMigrationSmokeTest` 会连接真实 MySQL 验证迁移记录和 `demo_user`、`demo_merchant`、`demo_admin` 演示账号 seed。

### 6.3 用户端 APP / Web

推荐使用项目脚本启动用户端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_user_app.ps1
```

也可以手动运行：

```powershell
cd apps/user_app
flutter pub get
flutter analyze
flutter test
```

Android 模拟器访问宿主机后端时：

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

桌面或浏览器调试：

```powershell
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

构建本地 Android Debug APK：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

本地 APK 文件名会按 `apps/user_app/pubspec.yaml` 的 `version` 生成版本化产物，例如：

```text
D:/aituan_release/apk/aituan-user-<version>-<build>-debug.apk
```

服务器 API 版 APK 可使用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk_server.ps1 -ServerOrigin "https://aituan.2b.gs"
```

服务器版脚本会同时维护下载入口使用的稳定文件名：

```text
D:/aituan_release/apk/aituan-user-server-debug.apk
deploy/artifacts/downloads/aituan-user-server-debug.apk
```

### 6.4 商家端 Web

```powershell
npm ci --prefix apps/merchant_web
npm run test:coverage --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

本地开发：

```powershell
npm run dev --prefix apps/merchant_web
```

默认开发端口由 Vite 配置维护，当前商家端为 `5174`。

### 6.5 后台端 Web

```powershell
npm ci --prefix apps/admin_web
npm run test:coverage --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

本地开发：

```powershell
npm run dev --prefix apps/admin_web
```

默认开发端口由 Vite 配置维护，当前后台端为 `5175`。

### 6.6 服务器版产物构建

服务器版手动构建总入口：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_all_server_artifacts.ps1 -ServerOrigin "https://aituan.2b.gs"
```

如只需要后端和 Web，不构建 APK：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_all_server_artifacts.ps1 -ServerOrigin "https://aituan.2b.gs" -SkipApk
```

该脚本会调用服务器版后端、前端和 APK 构建脚本，将产物放入 `deploy/artifacts/` 及 D 盘发布目录。注意：GitHub Actions CI/CD 不直接调用这些 Windows PowerShell 脚本，而是在 Linux runner 中按 workflow 内命令执行 Maven、npm、Flutter 构建。

### 6.7 本地完整 E2E

Playwright E2E 工程位于 `tests/e2e/`，当前覆盖 UC01-UC13。推荐本地一键运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1
```

或通过 npm 脚本：

```powershell
npm --prefix tests/e2e run stack:local
```

脚本会构建后端 JAR、用户端 Flutter Web、商家端 Web、后台端 Web，启动后端和本地静态服务后运行 `npx playwright test`。默认本地运行目录在 D 盘，避免把临时构建和日志写入 C 盘。

## 7. 测试

### 7.1 测试类型

| 端 / 层级 | 测试内容 | 工具 |
| --- | --- | --- |
| 静态回归 | 关键入口、文案和代码形态检查 | Bash + grep |
| 后端 | 服务层集成测试、API 集成测试、权限边界测试、覆盖率门禁 | JUnit 5、Spring Boot Test、MockMvc、H2、Flyway、JaCoCo |
| MySQL 迁移 | 空库迁移、Flyway 历史记录、演示账号 seed | MySQL 8、`MysqlMigrationSmokeTest` |
| 用户端 | 纯逻辑单测、Repository 测试、Widget 测试、静态分析、Web 构建 | flutter_test、flutter analyze、Flutter Web |
| 商家端 Web | API 层测试、覆盖率、类型检查、构建校验 | Vitest、happy-dom、vue-tsc、Vite |
| 后台端 Web | API 层测试、覆盖率、类型检查、构建校验 | Vitest、happy-dom、vue-tsc、Vite |
| 跨端 E2E | UC01-UC13 业务闭环 | Playwright、Chromium、后端 e2e profile、MySQL |

### 7.2 常用测试命令

静态回归：

```powershell
bash scripts/verify/member_e_regression_checks.sh
```

后端完整验证：

```powershell
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" verify
```

用户端：

```powershell
cd apps/user_app
flutter analyze
flutter test --coverage
flutter build web --base-href /web/
```

商家端：

```powershell
npm run test:coverage --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

后台端：

```powershell
npm run test:coverage --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

E2E：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1
```

### 7.3 CI 测试门禁

`.github/workflows/ci.yml` 当前在 PR 和手动触发时运行：

1. `static-regression`：执行 `scripts/verify/member_e_regression_checks.sh`。
2. `backend`：执行 `mvn verify`，上传 surefire / JaCoCo 报告，并通过 MySQL 8 service 运行 `MysqlMigrationSmokeTest`。
3. `web`：对 `apps/merchant_web`、`apps/admin_web` 矩阵执行依赖安装、`test:coverage` 和构建。
4. `flutter`：执行 `flutter pub get`、`flutter analyze`、`flutter test --coverage`、`flutter build web --base-href /web/`。
5. `e2e`：构建后端和三端 Web，启动 MySQL 与本地服务，运行 Playwright UC01-UC13。

默认 CI 已移除 Android emulator integration test，避免 GitHub Actions 模拟器启动不稳定导致主门禁长时间卡住；移动端真机/模拟器 `integration_test` 保留为手动或后续专项验证，不作为当前默认 CI 门禁。

### 7.4 当前测试覆盖

项目当前覆盖：

- 后端认证、权限、交易、优惠券、投诉、文件上传、评价、客服、会员成长值等高风险接口和服务。
- Flyway 在 H2 MySQL mode 和真实 MySQL service 下的迁移验证。
- Flutter 输入校验、JSON 解析、业务枚举、金额计算、Repository 请求路径和部分 Widget。
- 商家端 / 后台端集中式 API 层测试与覆盖率报告。
- Playwright UC01-UC13 跨端业务流程，包括登录、发现、个人资产、外卖下单、履约、券码、预约、退款、评价、投诉、客服、会员优惠券和商家目录维护。
- CI 中的 static-regression、backend、web、flutter、e2e 门禁。

详细报告见：

```text
docs/爱团测试报告.md
tests/e2e/README.md
docs/stage-new-1/测试与CICD补齐总清单.md
```

## 8. 部署

项目当前支持三类部署方式：

1. Kubernetes 主部署链路：当前课程 CI/CD 标准链路。
2. Docker Compose 单机/回退链路：适合快速恢复或无 K8s 环境。
3. 常规 JAR + MySQL + Nginx 手动安装：适合已有传统运维环境。

详细部署说明见：

```text
docs/爱团通用部署文档.md
deploy/README.md
k8s/README.md
docs/stage-new-1/Kubernetes部署与CICD说明.md
docs/stage-new-1/DockerCompose到K8s-CICD迁移简明说明.md
```

### 8.1 Kubernetes 主链路

Kubernetes manifests 位于：

```text
k8s/00-namespace.yaml
k8s/01-configmap.yaml
k8s/02-mysql.yaml
k8s/03-backend.yaml
k8s/04-web.yaml
k8s/05-ingress.yaml
k8s/secret.example.yaml
```

主流程：

```text
push main / 手动触发 aituan-deploy
  -> 测试、构建、E2E
  -> 构建 backend/web 镜像
  -> 推送 GHCR，tag 为 sha-短提交号
  -> kubectl apply K8s manifests
  -> kubectl set image 使用本次 sha tag
  -> kubectl rollout status 等待 MySQL/backend/web 发布成功
```

常用验收命令：

```bash
kubectl -n aituan get pods,svc,ingress
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
```

当前单节点 k3s 方案中，`k8s/04-web.yaml` 的 `web` Service 使用 `LoadBalancer` 接管 80/443；`k8s/05-ingress.yaml` 是安装 Ingress Controller 后的预留入口。K8s 模式下不要同机同时运行 Docker Compose Nginx，避免 80/443 端口冲突。

### 8.2 Docker Compose 回退部署

Compose 文件：

```text
deploy/docker-compose.server.yml
deploy/docker-compose.cicd.yml
deploy/docker-compose.acme.yml
```

手动产物部署通常使用：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

CI/CD 镜像化回退部署通常使用：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
```

`docker-compose.cicd.yml` 依赖 GHCR 镜像和 `AITUAN_IMAGE_REGISTRY`、`AITUAN_IMAGE_TAG`，部署 tag 与 GitHub Actions 计算出的 `sha-短提交号` 保持一致。

### 8.3 HTTPS

Nginx / K8s Web 镜像默认读取 Let's Encrypt 证书路径：

```text
/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem
/etc/letsencrypt/live/aituan.2b.gs/privkey.pem
```

Compose 模式下证书目录通常通过宿主机 `/etc/letsencrypt` 和 `/var/www/certbot` 挂载。K8s 模式下证书内容通过 `aituan-tls` Secret 挂载，仓库不保存证书内容。

如部署到其他域名，需要同步调整：

- GitHub Actions Variable `SERVER_ORIGIN`；
- Nginx server name / K8s ConfigMap 中的域名；
- 前端构建时的 `API_BASE_URL` / `VITE_API_BASE_URL`；
- TLS 证书和 Secret。

### 8.4 常规安装

常规安装方式适合已有 Java、MySQL、Nginx 运维环境的服务器：

1. 安装 Java 17、MySQL 8、Nginx；
2. 创建数据库和用户；
3. 构建后端 JAR；
4. 使用 systemd 或等价工具托管后端；
5. 将用户端 Web、商家端 Web、后台端 Web 静态产物交给 Nginx；
6. 配置 `/api/` 反向代理到后端端口；
7. 配置 HTTPS 和健康检查。

## 9. CI/CD

项目包含 GitHub Actions workflow：

| Workflow | 文件 | 作用 |
| --- | --- | --- |
| `aituan-ci` | `.github/workflows/ci.yml` | PR / 手动触发，运行静态回归、后端 verify + MySQL 迁移 smoke、Web coverage/build、Flutter analyze/test/build、Playwright UC01-UC13 E2E。 |
| `aituan-deploy` | `.github/workflows/deploy.yml` | main push / 手动触发，先跑测试与 E2E，再构建 backend/web 镜像，推送 GHCR，并按 `k8s`、`compose` 或 `none` 执行后续动作。 |
| `aituan-android-apk` | `.github/workflows/android-apk.yml` | 手动触发，构建用户端 Android debug APK，可选上传到服务器下载目录。 |

`aituan-deploy` 的部署目标：

```text
k8s     部署到 Kubernetes，当前默认目标
compose 回退到 Docker Compose
none    只测试、构建并推送镜像，不部署
```

镜像策略：

- Registry：`ghcr.io/<owner>/<repo>`。
- 镜像：`backend`、`web`。
- 部署 tag：`sha-${GITHUB_SHA::7}`。
- 同时推送 `main` tag 作为辅助标签，但部署以 sha tag 为准，不依赖 `latest`。

push main 自动部署需要同时满足：

```text
AUTO_DEPLOY_PRODUCTION=true
DEPLOY_TARGET=k8s 或 compose
```

如果 `DEPLOY_TARGET=none`，workflow 仍会完成测试、构建和镜像推送，但不会执行 K8s 或 Compose 部署。

## 10. 演示账号

演示账号由 seed 数据初始化，通常用于本地开发和课程展示。公开部署前应根据需要修改默认密码或禁用公开演示账号。

| 端 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 用户端 | `demo_user` | `123456` | 演示用户 |
| 商家端 | `demo_merchant` | `123456` | 基础商家演示账号 |
| 后台端 | `demo_admin` | `123456` | 平台管理员演示账号 |

## 11. 文档索引

- `docs/ReadMe.md`：完整文档索引。
- `docs/爱团通用部署文档.md`：通用部署说明。
- `deploy/README.md`：Docker Compose 部署与回退链路说明。
- `k8s/README.md`：Kubernetes manifests、Secret、rollout 和回滚说明。
- `tests/e2e/README.md`：Playwright UC01-UC13 端到端测试工程说明。
- `docs/stage-new-1/Kubernetes部署与CICD说明.md`：K8s 部署和 CI/CD 补齐说明。
- `docs/stage-new-1/DockerCompose到K8s-CICD迁移简明说明.md`：从 Compose 切换到 K8s 的课程验收说明。
- `docs/stage-new-1/测试与CICD补齐总清单.md`：测试与 CI/CD 补齐盘点。
- `docs/stage-new-2/微服务/微服务接口清单.md`：后续微服务拆分接口基线。
- `docs/爱团测试报告.md`：项目测试报告。
- `docs/stage-final/期末展示PPT大纲.md`：期末展示 PPT 大纲。
- `docs/stage6-memberE/AI助手交付说明.md`：AI 助手说明。
- `docs/stage1/API 分组设计.md`：API 分组设计。
- `docs/stage1/数据库表设计.md`：数据库设计。

## 12. 安全注意事项

1. 不要提交真实 `.config`、`deploy/.env`、数据库密码、JWT secret、AI key、邮箱授权码、SSH 私钥、kubeconfig、证书私钥或第三方 Token。
2. 公开部署必须设置强随机 `aituan.security.jwt-secret`。
3. 邮箱验证码调试返回默认应关闭，不应在公开环境直接向前端返回验证码。
4. 服务器部署前建议备份 `.config`、`deploy/.env`、数据库、K8s Secret 和旧产物。
5. 数据库结构更新统一通过 Flyway 迁移，不建议手动改表后不留脚本。
6. Flyway 迁移脚本必须兼容 MySQL 8 与 H2 `MODE=MySQL`，已执行过的迁移版本不要回改。
7. Docker Compose 与 K8s 镜像部署都建议使用 `sha-<短提交号>` 标签，便于追踪和回滚。
8. K8s `aituan-tls`、数据库 Secret、应用 `.config` Secret 必须预先在集群或 GitHub Secrets 中配置，不应写入仓库。
9. HTTPS 证书需要定期续期，续期后 reload Nginx 或滚动更新 Web Pod。
10. APK、部署压缩包、Playwright 报告、coverage、`deploy/artifacts/` 等构建产物默认不入仓库。

## 13. 后续可扩展方向

- 接入真实支付沙箱和更完整的资金清结算流程。
- 增加真实骑手端或更完整的配送轨迹模拟。
- 增加酒店房态、电影选座、技师排班等复杂预约能力。
- 提升 Playwright E2E 的并行度、稳定性、报告质量和失败定位能力。
- 将移动端 Android emulator / 真机 `integration_test` 作为手动专项或独立 workflow，避免阻塞默认 CI 门禁。
- 按 `docs/stage-new-2/微服务/微服务接口清单.md` 推进网关和微服务拆分。
- 扩展 AI Skills、调用日志、智能推荐和运营辅助能力。
