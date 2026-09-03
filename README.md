# 爱团本地生活服务综合平台

爱团是一个本地生活服务平台，覆盖用户消费、商家经营、平台治理和 AI 辅助。系统包含用户端 Flutter APP / Web、商家端 Web、后台端 Web、后端 API、自动化测试、Docker Compose 部署、Kubernetes/k3s 部署和 GitHub Actions CI/CD。

当前仓库保留两种后端运行形态：

- **单体版本**：`services/backend`。适合本地快速启动、功能演示、兼容性回归、单体基准测试和 Docker Compose 回退部署。
- **微服务版本**：`api-gateway` + A/B/C/D 四个业务微服务。适合生产主链路、独立构建测试、独立镜像发布、Kubernetes 部署、服务隔离和扩缩容。

## 1. 项目说明

### 1.1 核心定位

- 用户端：商家浏览、外卖点餐、团购/预约/票券购买、订单、券码、评价、客服、投诉、会员、优惠券、收藏、站内消息和 AI 助手。
- 商家端：门店资料、商品/服务管理、外卖履约、券码核销、评价回复、客服会话和经营概览。
- 后台端：商户治理、商品治理、订单治理、用户治理、会员优惠券、评价审核、投诉处理、平台客服、公告、审计日志和系统配置。
- 后端：REST API、JWT 鉴权、Flyway 迁移、幂等初始化数据、模拟支付、配送状态推进、券码核销、文件上传、跨服务调用和 AI 降级回复。

### 1.2 服务模块

| 模块 | 用户侧能力 | 履约方式 |
| --- | --- | --- |
| 外卖 | 商家浏览、商品点单、购物车、模拟支付、配送时间线 | 点单 + 配送模拟 |
| 团购 | 套餐浏览、购买、券码展示 | 到店券码核销 |
| 酒店 | 房型/服务展示、预约信息、凭证展示 | 预约 / 凭证使用 |
| 休闲娱乐 | 项目、套餐、时段和到店须知展示 | 预约 / 券码 |
| 电影演出 | 场次、票档、入场规则展示 | 票券核销 |
| 丽人医美 | 项目流程、服务时长、注意事项展示 | 预约服务 |
| 景点门票 | 票种、入园日期、开放时间展示 | 电子票核销 |
| 洗脚按摩 | 项目时长、到店/上门说明、预约时间展示 | 预约 / 服务完成 |

### 1.3 微服务划分

| 服务 | 端口 | 数据库 | 职责 |
| --- | ---: | --- | --- |
| `api-gateway` | 8080 | 无 | 统一 API 入口、外部路由、请求 ID、内部路径隔离 |
| `identity-asset-service` | 8081 | `aituan_identity` | 认证、账号、用户资料、地址、收藏、会员、优惠券、站内消息 |
| `merchant-catalog-service` | 8082 | `aituan_merchant` | 商家、门店、商品、SKU、库存、搜索、履约规则 |
| `trade-fulfillment-service` | 8083 | `aituan_trade` | 购物车、结算、订单、支付、退款、券码、预约、配送 |
| `engagement-platform-service` | 8084 | `aituan_platform` | 评价、投诉、客服、AI、公告、配置、审计、平台看板、文件 |

微服务版公开流量只经过 `api-gateway`。各业务服务的 `/internal/**` 接口只允许服务间调用，外部请求不能直接访问。

## 2. 环境版本

### 2.1 生产环境：`ssh aituan-weifuwu`

生产服务器登录口径统一为：

```bash
ssh aituan-weifuwu
```

生产公网入口：

```text
https://aituan.2b.gs
```

生产环境使用单节点 k3s 承载微服务版本，Docker Compose 保留为单体版本或临时回退方案。同一台服务器不要同时让 Kubernetes Web Service 和 Compose Nginx 占用 80/443。

| 项目 | 当前口径 |
| --- | --- |
| 服务器别名 | `aituan-weifuwu` |
| 应用目录 | `/opt/aituan/app` |
| 数据目录 | `/opt/aituan/data` |
| APK 下载目录 | `/opt/aituan/data/downloads` |
| Kubernetes 命名空间 | `aituan` |
| 主部署方式 | k3s + Kubernetes manifests + GHCR SHA 镜像 |
| 回退部署方式 | Docker Compose |
| TLS 证书路径 | `/etc/letsencrypt/live/aituan.2b.gs/` |
| 服务器构建策略 | 服务器不直接跑 Maven / npm / Flutter 构建，构建由 GitHub Actions 或本地脚本完成 |

在服务器上查看环境：

```bash
ssh aituan-weifuwu "hostname && cat /etc/os-release | head -n 5"
ssh aituan-weifuwu "docker --version && docker compose version"
ssh aituan-weifuwu "k3s --version || true"
ssh aituan-weifuwu "kubectl version --client=true"
ssh aituan-weifuwu "df -h /opt/aituan /opt/aituan/data"
```

### 2.2 本地开发环境

本地开发以 Windows 11 + PowerShell 为主。构建缓存、临时目录和产物优先放到 D 盘，避免占用 C 盘。

| 工具 | 推荐 / 当前版本 | 说明 |
| --- | --- | --- |
| Windows | Windows 11 | 本地优先使用 PowerShell 脚本 |
| Java | 17 | 后端和微服务编译目标 Java 17 |
| Maven | 3.9.14 | 建议使用 `D:/aituan_cache/m2` |
| Flutter | 3.41.6 stable | 用户端 APP / Web |
| Dart | 3.11.4 | `apps/user_app` SDK 约束为 `^3.11.4` |
| Node.js | 24.11.1 | 商家端、后台端、E2E |
| npm | 11.6.2 | 建议使用 `D:/aituan_cache/npm` |
| MySQL | 8.x | dev / CI / 部署数据库 |
| H2 | MySQL mode | 单体 demo/test 可用内存库 |
| Android SDK / Gradle | 由 Flutter / Android Studio 提供 | APK 构建使用 `D:/aituan_cache/gradle` |

### 2.3 项目关键依赖版本

| 端 / 层 | 关键版本 |
| --- | --- |
| 后端 / 微服务 | Spring Boot 3.4.6、Spring Cloud 2024.0.2、Springdoc 2.8.8、JaCoCo 0.8.12、Java 17 |
| 用户端 | Flutter 3.41.6、Dart 3.11.4、http 1.5.0、flutter_lints 6.0.0 |
| 商家端 Web | Vue 3.5.34、Vite 7.3.6、Vitest 4.1.8、happy-dom 20.10.2 |
| 后台端 Web | Vue 3.5.34、Vite 7.3.6、Vitest 4.1.8、happy-dom 20.10.2 |
| E2E | Playwright 1.62.1、TypeScript 5.9.3 |

## 3. 目录与端口

### 3.1 主要目录

```text
.
├─ apps/user_app                         Flutter 用户端 APP / Web
├─ apps/merchant_web                     Vue 商家端 Web
├─ apps/admin_web                        Vue 后台端 Web
├─ services/pom.xml                      后端多模块 Maven 父工程
├─ services/common-contract              公共契约模块
├─ services/api-gateway                  微服务 Gateway
├─ services/identity-asset-service       A：账号与用户资产服务
├─ services/merchant-catalog-service     B：商家与商品服务
├─ services/trade-fulfillment-service    C：交易与履约服务
├─ services/engagement-platform-service  D：互动与平台服务
├─ services/backend                      单体后端
├─ database/microservices                微服务四库迁移与初始化数据
├─ database/migrations                   单体 Flyway 版本迁移脚本
├─ database/seeds                        单体 repeatable 初始化数据
├─ scripts/build                         本地与服务器版构建脚本
├─ scripts/dev                           本地启动脚本
├─ scripts/verify                        测试、契约和报告脚本
├─ scripts/deploy                        部署辅助脚本
├─ tests/e2e                             Playwright 端到端测试工程
├─ tests/contracts                       跨服务契约测试
├─ tests/performance                     性能测试脚本和结果
├─ deploy                                Docker Compose、Nginx、镜像构建和静态产物目录
└─ k8s                                   Kubernetes manifests
```

### 3.2 单体版本本地端口

| 服务 | 默认地址 | 说明 |
| --- | --- | --- |
| 单体后端 API | `http://localhost:8080` | `SERVER_PORT` 可覆盖；demo profile 默认 H2 |
| 单体健康检查 | `http://localhost:8080/actuator/health` | Actuator health |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | 接口文档 |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | OpenAPI 描述 |
| H2 Console | `http://localhost:8080/h2-console` | 仅 demo profile 开启 |
| 商家端 Web 开发服务 | `http://localhost:5174` | Vite dev server |
| 后台端 Web 开发服务 | `http://localhost:5175` | Vite dev server |
| 本地 E2E 静态站点 | `http://127.0.0.1:8090` | 包含 `/web/`、`/merchant/`、`/admin/` |
| MySQL dev | `127.0.0.1:3306` | dev profile 默认库名 `aituan_dev` |

### 3.3 微服务版本本地端口

| 服务 | 宿主机端口 | 服务端口 | 说明 |
| --- | ---: | ---: | --- |
| `api-gateway` | 18080 | 8080 | 统一入口 |
| `identity-asset-service` | 18081 | 8081 | A 服务 |
| `merchant-catalog-service` | 18082 | 8082 | B 服务 |
| `trade-fulfillment-service` | 18083 | 8083 | C 服务 |
| `engagement-platform-service` | 18084 | 8084 | D 服务 |
| MySQL | Compose 内部 | 3306 | 四个逻辑 schema |

### 3.4 生产端口与路径

| 服务 | 地址 / 端口 | 说明 |
| --- | --- | --- |
| Web HTTP | `http://aituan.2b.gs` / `80` | Web 入口和 ACME HTTP-01 |
| Web HTTPS | `https://aituan.2b.gs` / `443` | 主入口 |
| 用户端 Web | `/web/` | Flutter Web |
| 商家端 Web | `/merchant/` | Vue 商家端 |
| 后台端 Web | `/admin/` | Vue 后台端 |
| APK 下载目录 | `/downloads/` | Web 容器读取 `/opt/aituan/data/downloads` |
| 单体版 API | `/api/` | 反向代理到 `backend:8080` |
| 微服务版 API | `/api/` | 反向代理到 `api-gateway:8080`，再路由到 A/B/C/D |
| 健康检查 | `/actuator/health` | 单体版检查后端；微服务版检查 Gateway |
| k3s API | `127.0.0.1:6443` | Actions 通过 SSH 隧道访问 |

## 4. 本地启动教程

### 4.1 准备 D 盘缓存和产物目录

常用目录如下，脚本会自动创建；手动执行命令时也建议沿用：

```text
D:/aituan_cache/m2/              Maven 缓存
D:/aituan_cache/pub/             Flutter Pub 缓存
D:/aituan_cache/gradle/          Gradle 缓存
D:/aituan_cache/npm/             npm 缓存
D:/aituan_release/backend/       后端 jar 输出
D:/aituan_release/apk/           APK 输出
D:/aituan_runtime/backend/       单体后端运行日志
D:/aituan_runtime/uploads/       本地上传文件
D:/aituan_runtime/e2e/           E2E 临时构建和日志
```

### 4.2 安装前端依赖

```powershell
npm ci --prefix apps/merchant_web --cache D:/aituan_cache/npm
npm ci --prefix apps/admin_web --cache D:/aituan_cache/npm

$env:PUB_CACHE = "D:/aituan_cache/pub"
$env:GRADLE_USER_HOME = "D:/aituan_cache/gradle"
Set-Location apps/user_app
flutter pub get
Set-Location ../..
```

### 4.3 启动单体后端 Demo 环境

Demo 环境默认使用 H2 内存数据库，会自动执行迁移和初始化数据，适合本地演示和接口联调。

一键脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_backend.ps1
```

手动启动：

```powershell
$env:JAVA_HOME = "D:/tools/jdk-17.0.18+8"
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:AITUAN_UPLOAD_ROOT = "D:/aituan_runtime/uploads"

mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" clean package
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

启动成功后检查：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/open/auth/token/check
```

浏览器可打开：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/h2-console
```

H2 Console 常用连接信息：

```text
JDBC URL: jdbc:h2:mem:aituan_demo
User Name: sa
Password: 留空
```

### 4.4 使用 MySQL 启动单体 dev 环境

先创建库和用户：

```sql
CREATE DATABASE aituan_dev
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'aituan'@'%' IDENTIFIED BY 'aituan_password';
GRANT ALL PRIVILEGES ON aituan_dev.* TO 'aituan'@'%';
FLUSH PRIVILEGES;
```

然后启动单体后端：

```powershell
$env:JAVA_HOME = "D:/tools/jdk-17.0.18+8"
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:AITUAN_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/aituan_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:AITUAN_DATASOURCE_USERNAME = "aituan"
$env:AITUAN_DATASOURCE_PASSWORD = "aituan_password"
$env:AITUAN_UPLOAD_ROOT = "D:/aituan_runtime/uploads"

mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" clean package
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

### 4.5 启动微服务本地版本

有 Docker Compose 的环境可一键启动完整微服务栈：

```bash
docker compose -f deploy/docker-compose.microservices.yml up -d --build
docker compose -f deploy/docker-compose.microservices.yml ps
deploy/microservices/acceptance-smoke.sh
```

查看日志：

```bash
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 api-gateway
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 identity-asset-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 merchant-catalog-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 trade-fulfillment-service
docker compose -f deploy/docker-compose.microservices.yml logs --tail=200 engagement-platform-service
```

仅做代码级构建或单服务开发时使用 Maven：

```powershell
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" clean test
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl api-gateway -am test
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl identity-asset-service -am test
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl merchant-catalog-service -am test
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl trade-fulfillment-service -am test
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl engagement-platform-service -am test
```

### 4.6 启动用户端 Flutter APP

脚本启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_user_app.ps1
```

Android 模拟器连接宿主机单体后端：

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Windows 桌面或浏览器连接本机单体后端：

```powershell
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

连接本地微服务 Gateway：

```powershell
flutter run --dart-define=API_BASE_URL=http://localhost:18080
```

构建本地 Android Debug APK：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

当前用户端版本来自 `apps/user_app/pubspec.yaml`：`1.1.7+24`。脚本会输出版本化 APK，例如：

```text
D:/aituan_release/apk/aituan-user-1.1.7-24-debug.apk
```

### 4.7 启动商家端 Web

```powershell
npm ci --prefix apps/merchant_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev --prefix apps/merchant_web
```

访问：

```text
http://localhost:5174
```

连接本地微服务 Gateway 时，将 `VITE_API_BASE_URL` 改为 `http://localhost:18080`。

### 4.8 启动后台端 Web

```powershell
npm ci --prefix apps/admin_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev --prefix apps/admin_web
```

访问：

```text
http://localhost:5175
```

连接本地微服务 Gateway 时，将 `VITE_API_BASE_URL` 改为 `http://localhost:18080`。

## 5. 健康检查与登录验证

### 5.1 本地单体健康检查

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/open/auth/token/check
```

期望返回 `UP` 或统一响应结构，说明后端 API 可访问。

### 5.2 本地微服务健康检查

```powershell
Invoke-RestMethod http://localhost:18080/actuator/health
Invoke-RestMethod http://localhost:18081/actuator/health
Invoke-RestMethod http://localhost:18082/actuator/health
Invoke-RestMethod http://localhost:18083/actuator/health
Invoke-RestMethod http://localhost:18084/actuator/health
```

微服务完整 smoke 会验证三角色登录、商品查询、下单支付履约、评价、成长值/消息、文件上传转发、Gateway 内部路径隔离和请求 ID。

### 5.3 生产健康检查

公网检查：

```powershell
Invoke-RestMethod https://aituan.2b.gs/actuator/health
```

服务器资源检查：

```bash
ssh aituan-weifuwu "kubectl -n aituan get pods,svc,hpa,pvc,ingress,deploy,statefulset -o wide"
ssh aituan-weifuwu "kubectl get nodes -o wide"
```

生产微服务逐个健康检查：

```bash
ssh aituan-weifuwu "kubectl -n aituan run health-gateway --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://api-gateway:8080/actuator/health"
ssh aituan-weifuwu "kubectl -n aituan run health-identity --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://identity-asset-service:8081/actuator/health"
ssh aituan-weifuwu "kubectl -n aituan run health-merchant --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://merchant-catalog-service:8082/actuator/health"
ssh aituan-weifuwu "kubectl -n aituan run health-trade --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://trade-fulfillment-service:8083/actuator/health"
ssh aituan-weifuwu "kubectl -n aituan run health-platform --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://engagement-platform-service:8084/actuator/health"
```

### 5.4 Web 入口检查

本地开发：

```text
http://localhost:5174
http://localhost:5175
```

生产环境：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
https://aituan.2b.gs/downloads/
```

浏览器 Network 中 API 请求应为 `https://aituan.2b.gs/api/...`，不应出现 `localhost`。

## 6. 测试教程

### 6.1 分类测试入口

```powershell
# 单元测试：后端服务层/工具类、Flutter、商家端 Web、后台端 Web
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_unit.ps1

# 集成/API 测试：后端 MockMvc、Controller、契约测试；默认跳过 MySQL smoke
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_integration_api.ps1

# 分类总入口：默认运行 unit + integration/API；需要 E2E 时追加 -IncludeE2E
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_all_classified.ps1

# 同时纳入 MySQL smoke 和 E2E
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_all_classified.ps1 -IncludeMysqlSmoke -IncludeE2E
```

### 6.2 单体后端测试

```powershell
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" verify
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" test
```

只验证 MySQL 迁移 smoke test 时，需要先启动 MySQL 8 并配置连接：

```powershell
$env:AITUAN_MYSQL_CI_ENABLED = "true"
$env:AITUAN_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/aituan_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:AITUAN_DATASOURCE_USERNAME = "aituan"
$env:AITUAN_DATASOURCE_PASSWORD = "aituan_password"

mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -Dtest=MysqlMigrationSmokeTest test
```

### 6.3 微服务测试

全量微服务 Maven reactor：

```powershell
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" clean verify
```

按服务独立测试：

```powershell
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl api-gateway -am clean verify
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl identity-asset-service -am clean verify
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl merchant-catalog-service -am clean verify
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl trade-fulfillment-service -am clean verify
mvn -B -f services/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" -pl engagement-platform-service -am clean verify
```

有 Docker Compose 的环境可运行真实容器验收：

```bash
docker compose -f deploy/docker-compose.microservices.yml config --quiet
docker compose -f deploy/docker-compose.microservices.yml up -d --build
deploy/microservices/acceptance-smoke.sh
bash scripts/verify/verify_mysql_schema_isolation.sh
```

### 6.4 用户端 Flutter 测试

```powershell
Set-Location apps/user_app
$env:PUB_CACHE = "D:/aituan_cache/pub"
$env:GRADLE_USER_HOME = "D:/aituan_cache/gradle"
flutter pub get
flutter analyze
flutter test --coverage
flutter build web --base-href /web/ --dart-define=API_BASE_URL=http://localhost:8080
Set-Location ../..
```

### 6.5 商家端 Web 测试

```powershell
npm ci --prefix apps/merchant_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run test:coverage --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

### 6.6 后台端 Web 测试

```powershell
npm ci --prefix apps/admin_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run test:coverage --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

### 6.7 本地完整 E2E 测试

一键运行端到端场景：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1
```

也可以从 E2E 工程执行 npm 脚本：

```powershell
Set-Location tests/e2e
npm ci
npm run stack:local
Set-Location ../..
```

脚本会自动完成后端 JAR 构建、三端 Web 构建、后端和静态服务启动、Playwright 执行，并在结束后关闭脚本启动的进程。

### 6.8 CI/CD 测试门禁

| Workflow | 触发方式 | 作用 |
| --- | --- | --- |
| `aituan-ci` | Pull Request / 手动触发 | 单体兼容测试、Web/Flutter 测试构建、端到端测试、原始报告 |
| `aituan-deploy` | push `main` / 手动触发 | 单体版镜像构建、K8s/Compose 回退部署、健康检查、原始报告 |
| `aituan-microservices-ci` | PR / push `main` / 手动触发 | 单体回归、五服务独立测试、四库 MySQL、契约、Gateway E2E、Compose/K8s 合约、七镜像可构建、原始报告 |
| `aituan-microservices-deploy` | `aituan-microservices-ci` 成功后 / 手动触发 | 校验同 SHA 完整 CI、复跑质量门禁、发布七镜像、部署 k3s、健康/版本/三端/三角色/APK 复验、原始报告 |
| `aituan-android-apk` | 手动触发 | 构建用户端 Android Debug APK，可选上传服务器下载目录 |

微服务部署不能绕过完整 CI。手动触发微服务部署时，目标 SHA 必须已有成功的微服务 CI 记录。

### 6.9 常用构建脚本汇总

| 目标 | 命令 |
| --- | --- |
| 单体后端本地 JAR | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_backend.ps1` |
| 启动单体后端 demo | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_backend.ps1` |
| 启动用户端 APP | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_user_app.ps1` |
| 本地 Android APK | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1` |
| 分类单元测试 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_unit.ps1` |
| 分类集成/API 测试 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_integration_api.ps1` |
| 分类测试总入口 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_all_classified.ps1` |
| 单体服务器版后端 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_backend_server.ps1` |
| 单体服务器版前端 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_frontends_server.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 单体服务器版 APK | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk_server.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 单体服务器版全部产物 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_all_server_artifacts.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 本地 E2E | `powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1` |

## 7. 测试账号

所有演示账号密码均为：`123456`。账号由初始化数据写入，重复启动时会幂等更新。

### 7.1 常用登录账号

| 端 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 用户端 | `demo_user` | `123456` | 演示消费者账号；手机 `18800001111`，邮箱 `user@example.com` |
| 商家端 | `demo_merchant` | `123456` | 基础商家账号 |
| 后台端 | `demo_admin` | `123456` | 平台管理员账号 |

### 7.2 分业务商家账号

| 账号 | 门店 / 业务 | 密码 |
| --- | --- | --- |
| `demo_takeaway_merchant` | 松记炸鸡饭 / 外卖 | `123456` |
| `demo_groupbuy_merchant` | 江南小馆 / 团购 | `123456` |
| `demo_hotel_merchant` | 云栖酒店 / 酒店 | `123456` |
| `demo_entertainment_merchant` | 星盒密室 / 休闲娱乐 | `123456` |
| `demo_movie_merchant` | 光影剧场 / 电影演出 | `123456` |
| `demo_beauty_merchant` | 轻颜护理 / 丽人医美 | `123456` |
| `demo_ticket_merchant` | 城市观景 / 景点门票 | `123456` |
| `demo_massage_merchant` | 雅境足道 / 洗脚按摩 | `123456` |
| `demo_bibimbap_merchant` | 米村拌饭 / 外卖 | `123456` |
| `demo_bbq_merchant` | 琥珀烤肉 / 团购 | `123456` |
| `demo_hotel_room_merchant` | 曼居影院酒店 / 酒店 | `123456` |
| `demo_arcade_merchant` | 趣动电玩城 / 休闲娱乐 | `123456` |
| `demo_spa_merchant` | 悦己 SPA / 丽人医美 | `123456` |

## 8. 初始数据说明

初始化数据由 Flyway repeatable seed 自动写入，重复启动或重复迁移时按幂等逻辑更新，不需要清库。

### 8.1 单体版本

单体版本使用 `aituan_dev` 或 H2 内存库，主要数据包括账号、角色、门店、商品、订单、券码、配送任务、评价、客服、投诉、消息、会员等级、优惠券模板和系统配置。

### 8.2 微服务版本

微服务版本按服务拆分为四个 schema：

| 服务 | Schema | 主要数据 |
| --- | --- | --- |
| A：`identity-asset-service` | `aituan_identity` | 账号、用户资料、地址、收藏、会员、优惠券、站内消息 |
| B：`merchant-catalog-service` | `aituan_merchant` | 商家、门店、商品、SKU、库存、搜索、履约规则 |
| C：`trade-fulfillment-service` | `aituan_trade` | 购物车、结算、订单、支付、退款、券码、预约、配送 |
| D：`engagement-platform-service` | `aituan_platform` | 评价、投诉、客服、AI、公告、配置、审计、平台看板、文件 |

微服务数据边界：

- 每张业务表只能归属一个服务。
- 服务只能连接自己的 schema 和账号。
- 禁止跨 schema JOIN、跨 schema FK、跨服务直接读写表。
- 跨服务读取或写入必须通过内部 API，并携带内部服务 Token、调用方、请求 ID 和必要的幂等键。
- 迁移脚本需要同时兼容 MySQL 8 与 H2 MySQL mode 空库初始化。

### 8.3 默认门店与业务数据

初始化门店覆盖八类业务：外卖、团购、酒店、休闲娱乐、电影演出、丽人医美、景点门票、洗脚按摩。默认数据还包括商品分类、SKU、推荐位、外卖配送规则、商家接单模式、用户订单、券码、预约、评价、客服会话、投诉工单、站内消息、公告、会员等级和优惠券。

## 9. 生产环境启动教程

### 9.1 生产部署总览

推荐生产链路是微服务 Kubernetes：

```text
push main
  -> aituan-microservices-ci
     -> 单体回归、五服务测试、四库 MySQL、契约、Gateway E2E、K8s/Compose 合约、七镜像构建检查
  -> aituan-microservices-deploy
     -> 校验同 SHA 完整 CI
     -> 复跑 Java / Vue / Flutter 质量门禁
     -> 发布 api-gateway + A/B/C/D + mysql + web 共 7 个 SHA 镜像
     -> 部署到 aituan-weifuwu 的 k3s
     -> 验证 5 个 Java 服务 health/info、7 个镜像 tag、三端页面、三角色登录和 APK 下载
```

单体部署仍可用于旧版演示、对比实验或临时回退；微服务部署是当前主线。

### 9.2 微服务 Kubernetes 首次启动

登录服务器并进入应用目录：

```bash
ssh aituan-weifuwu
cd /opt/aituan/app
```

创建或更新基础资源：

```bash
kubectl apply -f k8s/microservices/common.yaml
```

准备必需 Secret：

| Secret | 字段 | 说明 |
| --- | --- | --- |
| `aituan-mysql-root-secret` | `password` | MySQL root 密码 |
| `aituan-identity-db-secret` | `username`, `password` | A 服务数据库账号 |
| `aituan-merchant-db-secret` | `username`, `password` | B 服务数据库账号 |
| `aituan-trade-db-secret` | `username`, `password` | C 服务数据库账号 |
| `aituan-platform-db-secret` | `username`, `password` | D 服务数据库账号 |
| `aituan-service-secret` | `jwt-secret`, `internal-service-token` | JWT 和服务间调用 Token |
| `aituan-app-config` | `.config` | 应用业务配置 |
| `aituan-tls` | `tls.crt`, `tls.key` | HTTPS 证书 |
| `ghcr-pull-secret` | docker registry auth | 私有 GHCR 镜像需要 |

手动创建数据库 Secret 示例：

```bash
kubectl -n aituan create secret generic aituan-mysql-root-secret \
  --from-literal=password='<真实 root 密码>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic aituan-identity-db-secret \
  --from-literal=username='aituan_identity_svc' \
  --from-literal=password='<A库密码>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic aituan-merchant-db-secret \
  --from-literal=username='aituan_merchant_svc' \
  --from-literal=password='<B库密码>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic aituan-trade-db-secret \
  --from-literal=username='aituan_trade_svc' \
  --from-literal=password='<C库密码>' \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic aituan-platform-db-secret \
  --from-literal=username='aituan_platform_svc' \
  --from-literal=password='<D库密码>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

创建应用配置和服务密钥：

```bash
cat > /tmp/aituan-app.config <<'EOF'
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.internal.service-token=<生产强随机内部服务 token>
aituan.mail.enabled=false
aituan.mail.debug-return-code=false
aituan.ai.enabled=false
aituan.upload.strategy=local
aituan.map.provider=local
EOF

kubectl -n aituan create secret generic aituan-app-config \
  --from-file=.config=/tmp/aituan-app.config \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n aituan create secret generic aituan-service-secret \
  --from-literal=jwt-secret='<同上 JWT secret>' \
  --from-literal=internal-service-token='<同上内部服务 token>' \
  --dry-run=client -o yaml | kubectl apply -f -

rm -f /tmp/aituan-app.config
```

创建 TLS Secret：

```bash
kubectl -n aituan create secret tls aituan-tls \
  --cert=/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem \
  --key=/etc/letsencrypt/live/aituan.2b.gs/privkey.pem \
  --dry-run=client -o yaml | kubectl apply -f -
```

应用微服务拓扑：

```bash
kubectl apply -k k8s/microservices
```

正式发布时不要直接使用占位镜像，应通过 GitHub Actions 或手动渲染替换为同一个提交的不可变 SHA tag。

### 9.3 微服务 Kubernetes 日常更新和回滚

查看状态：

```bash
ssh aituan-weifuwu "kubectl -n aituan get pods,svc,hpa,pvc,ingress,deploy,statefulset -o wide"
```

等待滚动发布：

```bash
ssh aituan-weifuwu "kubectl -n aituan rollout status statefulset/mysql --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/api-gateway --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/identity-asset-service --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/merchant-catalog-service --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/trade-fulfillment-service --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/engagement-platform-service --timeout=600s"
ssh aituan-weifuwu "kubectl -n aituan rollout status deployment/aituan-web --timeout=600s"
```

重启某个服务：

```bash
ssh aituan-weifuwu "kubectl -n aituan rollout restart deployment/api-gateway"
ssh aituan-weifuwu "kubectl -n aituan rollout restart deployment/merchant-catalog-service"
```

查看日志：

```bash
ssh aituan-weifuwu "kubectl -n aituan logs deployment/api-gateway --tail=200"
ssh aituan-weifuwu "kubectl -n aituan logs deployment/identity-asset-service --tail=200"
ssh aituan-weifuwu "kubectl -n aituan logs deployment/merchant-catalog-service --tail=200"
ssh aituan-weifuwu "kubectl -n aituan logs deployment/trade-fulfillment-service --tail=200"
ssh aituan-weifuwu "kubectl -n aituan logs deployment/engagement-platform-service --tail=200"
```

回滚到上一个 ReplicaSet：

```bash
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/api-gateway"
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/identity-asset-service"
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/merchant-catalog-service"
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/trade-fulfillment-service"
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/engagement-platform-service"
ssh aituan-weifuwu "kubectl -n aituan rollout undo deployment/aituan-web"
```

### 9.4 GitHub Actions 微服务发布配置

Repository Variables：

| Variable | 建议值 | 说明 |
| --- | --- | --- |
| `SERVER_ORIGIN` | `https://aituan.2b.gs` | 站点 origin，不要带 `/api` |
| `AUTO_DEPLOY_PRODUCTION` | `true` | `main` 完整 CI 通过后是否自动生产部署 |
| `DEPLOY_TARGET` | `k8s` | 默认生产部署目标，可选 `k8s`、`compose`、`none` |
| `K8S_NAMESPACE` | `aituan` | Kubernetes 命名空间 |
| `SERVER_APP_DIR` | `/opt/aituan/app` | 服务器部署目录 |

Production Secrets：

| Secret | 用途 |
| --- | --- |
| `KUBE_CONFIG` | 连接 `aituan-weifuwu` k3s 集群的 kubeconfig 内容 |
| `SERVER_HOST` / `SERVER_PORT` / `SERVER_USER` | Actions 通过 SSH 连接服务器和转发 k3s API |
| `SERVER_SSH_KEY` / `SERVER_KNOWN_HOSTS` | SSH 私钥和 known_hosts |
| `K8S_MYSQL_ROOT_PASSWORD` | MySQL root Secret |
| `K8S_IDENTITY_DB_PASSWORD` | A 服务数据库密码 |
| `K8S_MERCHANT_DB_PASSWORD` | B 服务数据库密码 |
| `K8S_TRADE_DB_PASSWORD` | C 服务数据库密码 |
| `K8S_PLATFORM_DB_PASSWORD` | D 服务数据库密码 |
| `K8S_MYSQL_PASSWORD` | 兼容旧变量；没有分库密码时作为回退 |
| `K8S_APP_CONFIG` | 完整应用配置内容 |
| `GHCR_PULL_USERNAME` / `GHCR_PULL_TOKEN` | 私有 GHCR 镜像拉取账号和 token |

手动触发 `aituan-microservices-deploy`：

1. 打开 GitHub Actions。
2. 选择 `aituan-microservices-deploy`。
3. 点击 `Run workflow`。
4. `deploy=true`。
5. `deploy_target=k8s` 表示部署到生产 k3s。
6. `deploy_target=compose` 表示走 Compose 回退链路。
7. `deploy_target=none` 表示只做质量门禁和镜像发布，不发布。

### 9.5 单体版本 Docker Compose 部署

单体版本适合旧版演示、对比实验或微服务故障时临时回退。先登录服务器：

```bash
ssh aituan-weifuwu
cd /opt/aituan/app
```

准备 `deploy/.env`，不要提交真实文件：

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=<真实 MySQL 用户>
MYSQL_PASSWORD=<真实 MySQL 密码>
MYSQL_ROOT_PASSWORD=<真实 MySQL root 密码>
AITUAN_DATA_DIR=/opt/aituan/data
AITUAN_CONFIG_HOST_FILE=../.config
AITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan
AITUAN_IMAGE_TAG=sha-xxxxxxx
AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads
AITUAN_NGINX_SERVER_NAME=aituan.2b.gs
AITUAN_LETSENCRYPT_DIR=/etc/letsencrypt
AITUAN_CERTBOT_WEBROOT=/var/www/certbot
```

准备应用配置，保存为服务器 `/opt/aituan/app/.config`，不要提交真实文件：

```properties
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.mail.enabled=false
aituan.mail.debug-return-code=false
aituan.ai.enabled=false
aituan.upload.strategy=local
aituan.map.provider=local
```

创建目录：

```bash
mkdir -p /opt/aituan/data/mysql /opt/aituan/data/uploads /opt/aituan/data/downloads /var/www/certbot/.well-known/acme-challenge /etc/letsencrypt
```

使用 CI/CD 镜像化 Compose：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml ps
curl -fsS http://127.0.0.1/actuator/health
```

使用服务器本地静态产物和本地构建镜像：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

停止单体 Compose：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml down
```

### 9.6 单体服务器版产物构建

本地 Windows 构建服务器版产物：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_backend_server.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_frontends_server.ps1 -ServerOrigin "https://aituan.2b.gs"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk_server.ps1 -ServerOrigin "https://aituan.2b.gs"
```

一次性构建全部：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_all_server_artifacts.ps1 -ServerOrigin "https://aituan.2b.gs"
```

主要输出：

```text
deploy/artifacts/backend/aituan-backend.jar
deploy/artifacts/landing
deploy/artifacts/user-web
deploy/artifacts/merchant-web
deploy/artifacts/admin-web
D:/aituan_release/apk/aituan-user-server-debug.apk
deploy/artifacts/downloads/aituan-user-server-debug.apk
```

### 9.7 APK 发布和下载

手动触发 `aituan-android-apk`：

| 输入 | 建议值 | 说明 |
| --- | --- | --- |
| `api_origin` | 留空或 `https://aituan.2b.gs` | 留空时使用 `SERVER_ORIGIN` |
| `upload_to_server` | `true` | 是否上传到服务器下载目录 |
| `apk_name` | `aituan-user-server-debug.apk` 或留默认 | 默认会按版本号生成 APK |

生产下载入口：

```text
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

## 10. 配置要点

### 10.1 不能提交的敏感信息

不要把以下内容提交到 Git：

- SSH 密码、私钥、kubeconfig、known_hosts 中的私有信息。
- `deploy/.env`。
- `.config`。
- MySQL root 密码和四个服务数据库密码。
- JWT secret、内部服务 Token。
- 邮箱授权码、地图 Key、AI Key、图床 Token。
- TLS 证书私钥。
- GitHub / GHCR Token。

仓库只保留模板或占位示例，真实配置只保存在本地、服务器或 GitHub Secrets 中。

### 10.2 应用配置 `.config`

本地没有 `.config` 时，单体 demo 可使用默认值启动；公开部署必须设置强随机 JWT secret。微服务部署还需要内部服务 Token。

```properties
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.internal.service-token=<生产强随机内部服务 token>

aituan.ai.enabled=false
aituan.ai.api-url=http://cliapi.2b.gs
aituan.ai.api-key=
aituan.ai.model=pp/gpt-5.5

aituan.mail.enabled=false
aituan.mail.debug-return-code=false
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.starttls.enable=false
aituan.mail.from=
aituan.mail.from-name=爱团

aituan.upload.strategy=local
aituan.upload.root-dir=/data/uploads
aituan.map.provider=local
```

### 10.3 单体 Compose `deploy/.env`

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=<真实 MySQL 用户>
MYSQL_PASSWORD=<真实 MySQL 密码>
MYSQL_ROOT_PASSWORD=<真实 MySQL root 密码>
AITUAN_DATA_DIR=/opt/aituan/data
AITUAN_CONFIG_HOST_FILE=../.config
AITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan
AITUAN_IMAGE_TAG=sha-xxxxxxx
AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads
AITUAN_NGINX_SERVER_NAME=aituan.2b.gs
AITUAN_LETSENCRYPT_DIR=/etc/letsencrypt
AITUAN_CERTBOT_WEBROOT=/var/www/certbot
```

### 10.4 微服务部署配置

微服务 Kubernetes 以 `k8s/microservices` 为 Kustomize 入口：

```bash
kubectl apply -k k8s/microservices
```

核心规则：

- 5 个 Java 服务、MySQL 和 Web 都使用同一个提交的 `sha-<commit>` 镜像标签。
- `aituan-app-config` 保存应用配置，`aituan-service-secret` 保存 JWT 和内部服务 Token。
- 四个业务服务分别读取自己的 DB Secret，不复用同一个数据库账号。
- `aituan-downloads` 绑定服务器 `/opt/aituan/data/downloads`，APK 上传后 Web 可直接下载。
- Web 只读挂载 `/var/www/certbot` 用于 HTTP-01 challenge。
- Let's Encrypt 续期后需要把新证书同步到 `aituan-tls` 并滚动重启 Web。

### 10.5 数据、证书和备份

- 单体 MySQL 数据：`/opt/aituan/data/mysql`。
- 微服务 MySQL 数据：Kubernetes MySQL StatefulSet PVC。
- 上传文件：单体 Compose 使用 `/opt/aituan/data/uploads`；微服务中 D 服务使用独立持久化卷。
- APK 下载：`/opt/aituan/data/downloads`。
- TLS 证书：`/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem` 和 `privkey.pem`。
- 数据库结构变更统一走 Flyway 迁移；已经执行过的迁移版本不要直接改内容。
