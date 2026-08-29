# 爱团本地生活服务综合平台

爱团是一个生活助手平台。系统面向本地生活服务场景，覆盖用户消费、商家经营、平台治理和 AI 辅助，包含用户端 Flutter APP / Web、商家端 Web、后台端 Web、Spring Boot 后端、MySQL / H2 数据库、自动化测试、Docker Compose 回退部署、Kubernetes 生产部署和 GitHub Actions CI/CD。

## 1. 项目说明

### 1.1 核心定位

- 用户端：商家浏览、外卖点餐、团购/预约/票券购买、订单、券码、评价、客服、投诉、会员、优惠券和 AI 助手。
- 商家端：门店资料、商品/服务管理、外卖履约、券码核销、评价回复、客服会话和经营概览。
- 后台端：商户治理、商品治理、订单治理、用户治理、会员优惠券、评价审核、投诉处理、平台客服、公告、审计日志和系统配置。
- 后端：统一 REST API、JWT 鉴权、Flyway 迁移、幂等 seed、模拟支付、配送状态推进、券码核销、文件上传和 AI 降级回复。

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

## 2. 环境版本

### 2.1 生产环境：`ssh aituan-new`

生产环境以 `aituan-new` 服务器为准。当前主部署链路是单节点 k3s，Docker Compose 保留为回退部署方案。服务器只需要容器运行与集群管理工具，Java / Maven / Node / npm 不在服务器上直接构建使用，构建由 GitHub Actions 或本地脚本完成。

| 项目 | 当前环境 |
| --- | --- |
| 操作系统 | Debian GNU/Linux 11 bullseye |
| 内核 | Linux 5.10.0-15-amd64 x86_64 |
| Docker | 20.10.5+dfsg1 |
| Docker Compose | v5.5.0 |
| k3s | v1.36.3+k3s1 |
| kubectl | v1.36.3+k3s1 |
| containerd | 2.3.2-k3s2 |
| Java / Maven | 服务器未安装；由 CI 或本地构建镜像 / JAR |
| Node / npm | 服务器未安装；由 CI 或本地构建 Web 产物 |
| MySQL | Kubernetes / Compose 中使用 `mysql:8.0` 容器 |
| Nginx | Web 镜像内提供，不使用宿主机 Nginx |

### 2.2 本地开发环境

本地开发仍以 Windows 11 + PowerShell 为主。

| 工具 | 推荐 / 当前版本 | 说明 |
| --- | --- | --- |
| Windows | Windows 11 | 本地优先使用 PowerShell 脚本 |
| Java | 17 | 后端编译目标 Java 17 |
| Maven | 3.9.14 |  |
| Flutter | 3.41.6 stable | 用户端 APP / Web |
| Dart | 3.11.4 | `apps/user_app` SDK 约束为 `^3.11.4` |
| Node.js | 24.11.1 | 商家端、后台端、E2E |
| npm | 11.6.2 |  |
| MySQL | 8.x | dev / CI / 部署数据库 |
| H2 | MySQL mode | demo、test、默认本地 E2E 可使用内存库 |
| Android SDK / Gradle | 由 Flutter / Android Studio 提供 |  |

### 2.3 项目关键依赖版本

| 端 / 层 | 关键版本 |
| --- | --- |
| 后端 | Spring Boot 3.4.6、Springdoc 2.8.8、JaCoCo 0.8.12、Java 17 |
| 用户端 | Flutter 3.41.6、Dart 3.11.4、http 1.5.0、flutter_lints 6.0.0 |
| 商家端 Web | Vue 3.5.34、Vite 7.2.7、Vitest 4.1.8、happy-dom 20.10.2 |
| 后台端 Web | Vue 3.5.34、Vite 7.2.7、Vitest 4.1.8、happy-dom 20.10.2 |
| E2E | Playwright 1.62.1、TypeScript 5.9.3 |

## 3. 目录与端口

### 3.1 主要目录

```text
.
├─ apps/user_app          Flutter 用户端 APP / Web
├─ apps/merchant_web      Vue 商家端 Web
├─ apps/admin_web         Vue 后台端 Web
├─ services/backend       Spring Boot 后端
├─ database/migrations    Flyway 版本迁移脚本
├─ database/seeds         Flyway repeatable 初始演示数据
├─ scripts/build          本地与服务器版构建脚本
├─ scripts/dev            本地启动脚本
├─ scripts/verify         静态回归验证脚本
├─ tests/e2e              Playwright 端到端测试工程
├─ deploy                 Docker Compose、Nginx、镜像构建和静态产物目录
└─ k8s                    Kubernetes manifests
```

### 3.2 本地端口

| 服务 | 默认地址 | 说明 |
| --- | --- | --- |
| 后端 API | `http://localhost:8080` | `SERVER_PORT` 可覆盖，默认 profile 为 `demo` |
| 后端健康检查 | `http://localhost:8080/actuator/health` | Actuator health |
| 后端接口文档 | `http://localhost:8080/swagger-ui.html` | Swagger UI |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | OpenAPI 描述 |
| H2 Console | `http://localhost:8080/h2-console` | 仅 demo profile 开启 |
| 用户端 Flutter APP | Android 模拟器 / 桌面 / 浏览器 | Android 模拟器默认使用 `10.0.2.2:8080` 访问宿主机 |
| 商家端 Web 开发服务 | `http://localhost:5174` | Vite dev server |
| 后台端 Web 开发服务 | `http://localhost:5175` | Vite dev server |
| 本地 E2E 静态站点 | `http://127.0.0.1:8090` | E2E 脚本启动，包含 `/web/`、`/merchant/`、`/admin/` |
| MySQL dev | `127.0.0.1:3306` | dev profile 默认库名 `aituan_dev` |

### 3.3 生产端口与路径

| 服务 | 地址 / 端口 | 说明 |
| --- | --- | --- |
| k3s API | `:6443` | Kubernetes API server |
| Web HTTP | `http://aituan.2b.gs` / `80` | Web Service LoadBalancer |
| Web HTTPS | `https://aituan.2b.gs` / `443` | Web Service LoadBalancer + TLS Secret |
| 用户端 Web | `/web/` | Flutter Web |
| 商家端 Web | `/merchant/` | Vue 商家端 |
| 后台端 Web | `/admin/` | Vue 后台端 |
| APK 下载目录 | `/downloads/` | Web 容器挂载下载目录 |
| 后端 API | `/api/` | Web 容器反向代理到 `backend:8080` |
| 生产健康检查 | `/actuator/health` | Web 容器反向代理到 `backend:8080/actuator/health` |
| 集群内后端 | `backend.aituan.svc.cluster.local:8080` | ClusterIP Service |
| 集群内 MySQL | `mysql.aituan.svc.cluster.local:3306` | ClusterIP Service |

## 4. 本地启动教程

### 4.1 准备 D 盘缓存和产物目录

常用目录如下，脚本会自动创建，手动执行命令时也建议沿用：

```text
D:/aituan_cache/m2/              Maven 缓存
D:/aituan_cache/pub/             Flutter Pub 缓存
D:/aituan_cache/gradle/          Gradle 缓存
D:/aituan_cache/npm/             npm 缓存
D:/aituan_release/backend/       后端 jar 输出
D:/aituan_release/apk/           APK 输出
D:/aituan_runtime/backend/       后端运行日志
D:/aituan_runtime/uploads/       本地上传文件
D:/aituan_runtime/e2e/           E2E 临时构建和日志
```

### 4.2 启动后端 Demo 环境

Demo 环境默认使用 H2 内存数据库，会自动执行 Flyway 迁移和 seed 初始数据，适合本地演示和接口联调。

一键脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_backend.ps1
```

如果脚本里的本机 Java 路径不可用，使用下面的手动命令：

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

### 4.3 使用 MySQL 启动后端 dev 环境

如果要验证真实 MySQL 8，请先创建库和用户：

```sql
CREATE DATABASE aituan_dev
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'aituan'@'%' IDENTIFIED BY 'aituan_password';
GRANT ALL PRIVILEGES ON aituan_dev.* TO 'aituan'@'%';
FLUSH PRIVILEGES;
```

然后启动后端：

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

表结构和初始演示数据由 Flyway 自动初始化，不需要手工建表。

### 4.4 启动用户端 Flutter APP

脚本启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_user_app.ps1
```

手动启动前先取依赖和检查：

```powershell
$env:PUB_CACHE = "D:/aituan_cache/pub"
$env:GRADLE_USER_HOME = "D:/aituan_cache/gradle"

cd apps/user_app
flutter pub get
flutter analyze
flutter test
```

Android 模拟器连接宿主机后端：

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Windows 桌面或浏览器连接本机后端：

```powershell
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

构建本地 Android Debug APK：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

当前版本来自 `apps/user_app/pubspec.yaml`：`1.1.7+24`。脚本会输出版本化 APK，例如：

```text
D:/aituan_release/apk/aituan-user-1.1.7-24-debug.apk
```

### 4.5 启动商家端 Web

```powershell
npm ci --prefix apps/merchant_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev --prefix apps/merchant_web
```

访问：

```text
http://localhost:5174
```

### 4.6 启动后台端 Web

```powershell
npm ci --prefix apps/admin_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev --prefix apps/admin_web
```

访问：

```text
http://localhost:5175
```

## 5. 健康检查与登录验证

### 5.1 本地后端健康检查

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

期望返回类似：

```json
{"status":"UP"}
```

Open API 探活：

```powershell
Invoke-RestMethod http://localhost:8080/api/open/auth/token/check
```

该接口不要求登录，能返回统一响应结构即可说明后端 API 已可访问。

### 5.2 生产健康检查

在本机直接检查公网：

```powershell
Invoke-RestMethod http://aituan.2b.gs/actuator/health
Invoke-RestMethod https://aituan.2b.gs/actuator/health
```

在服务器上检查集群外入口：

```bash
ssh aituan-new "curl -fsS http://127.0.0.1/actuator/health"
```

在服务器上检查集群内后端 Service：

```bash
ssh aituan-new "kubectl -n aituan run aituan-health-check --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- curl -fsS http://backend:8080/actuator/health"
```

查看生产资源状态：

```bash
ssh aituan-new "kubectl -n aituan get pods,svc,ingress,deploy,statefulset"
ssh aituan-new "kubectl get nodes -o wide"
```

### 5.3 Web 入口检查

本地开发：

```text
http://localhost:5174
http://localhost:5175
```

本地 E2E 静态服务启动后：

```text
http://127.0.0.1:8090/web/
http://127.0.0.1:8090/merchant/
http://127.0.0.1:8090/admin/
```

生产环境：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
https://aituan.2b.gs/downloads/
```

## 6. 测试教程

如需按测试类型统一运行，优先使用分类脚本：

```powershell
# 单元测试：后端服务层/工具类、Flutter、商家端 Web、后台端 Web
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_unit.ps1

# 集成/API 测试：后端 MockMvc、Controller、契约测试；默认跳过 MySQL smoke
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_integration_api.ps1

# 分类总入口：默认运行 unit + integration/API；需要 E2E 时追加 -IncludeE2E
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_all_classified.ps1
```

详细清单见 `docs/stage-new-1/单元与集成测试分类运行说明.md`。

### 6.1 后端测试

运行后端测试和覆盖率门禁：

```powershell
mvn -B -f services/backend/pom.xml "-Dmaven.repo.local=D:/aituan_cache/m2" verify
```

只跑测试：

```powershell
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

### 6.2 用户端 Flutter 测试

```powershell
cd apps/user_app
$env:PUB_CACHE = "D:/aituan_cache/pub"
$env:GRADLE_USER_HOME = "D:/aituan_cache/gradle"

flutter pub get
flutter analyze
flutter test --coverage
flutter build web --base-href /web/ --dart-define=API_BASE_URL=http://localhost:8080
```

### 6.3 商家端 Web 测试

```powershell
npm ci --prefix apps/merchant_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run test:coverage --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

### 6.4 后台端 Web 测试

```powershell
npm ci --prefix apps/admin_web --cache D:/aituan_cache/npm
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run test:coverage --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

### 6.6 本地完整 E2E 测试

一键运行 UC01-UC13 端到端场景：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1
```

也可以从 E2E 工程执行 npm 脚本：

```powershell
cd tests/e2e
npm ci
npm run stack:local
```

脚本会自动完成：

1. 构建后端 JAR；
2. 构建用户端 Flutter Web；
3. 构建商家端 Web；
4. 构建后台端 Web；
5. 启动后端 `http://127.0.0.1:8080`；
6. 启动静态站点 `http://127.0.0.1:8090`；
7. 运行 Playwright UC01-UC13；
8. 结束后关闭脚本启动的后端和静态服务。

E2E 常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AITUAN_E2E_ROOT` | `D:/aituan_runtime/e2e` | E2E 临时构建与日志根目录 |
| `AITUAN_M2_REPO` | `D:/aituan_cache/m2` | Maven 本地仓库 |
| `AITUAN_JAVA_HOME` | `D:/tools/jdk-17.0.18+8` | JDK 17 路径 |
| `AITUAN_MAVEN` | `D:/tools/apache-maven-3.9.14/bin/mvn.cmd` | Maven 命令路径 |
| `E2E_API_ORIGIN` | `http://127.0.0.1:8080` | E2E API 地址 |
| `E2E_WEB_ORIGIN` | `http://127.0.0.1:8090` | E2E Web 地址 |
| `PLAYWRIGHT_BROWSER_PATH` | Microsoft Edge 路径 | 本地默认使用 Edge 可执行文件 |

### 6.6 常用构建脚本汇总

| 目标 | 命令 |
| --- | --- |
| 后端本地 JAR | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_backend.ps1` |
| 启动后端 demo | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_backend.ps1` |
| 启动用户端 APP | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev/start_user_app.ps1` |
| 本地 Android APK | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1` |
| 分类单元测试 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_unit.ps1` |
| 分类集成/API 测试 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_integration_api.ps1` |
| 分类测试总入口 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify/test_all_classified.ps1` |
| 服务器版后端 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_backend_server.ps1` |
| 服务器版前端 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_frontends_server.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 服务器版 APK | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk_server.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 服务器版全部产物 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_all_server_artifacts.ps1 -ServerOrigin "https://aituan.2b.gs"` |
| 本地 E2E | `powershell -NoProfile -ExecutionPolicy Bypass -File tests/e2e/scripts/run-e2e-local.ps1` |

## 7. 测试账号

所有演示账号密码均为：`123456`。账号由 seed 初始化，demo / test / e2e / dev profile 启动时会通过 Flyway 自动写入。

### 7.1 常用登录账号

| 端 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 用户端 | `demo_user` | `123456` | 演示消费者账号 |
| 商家端 | `demo_merchant` | `123456` | 基础商家账号，对应塔斯汀中国汉堡 |
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
| `demo_spa_merchant` | 悦己SPA / 丽人医美 | `123456` |

## 8. 初始数据说明

初始数据由 Flyway repeatable seed 自动写入，重复启动或重复迁移时会按幂等逻辑更新，不需要清库。

### 8.1 账号与角色

- 角色：`USER`、`MERCHANT`、`ADMIN`。
- 用户：`demo_user`，昵称“爱团用户”，白银会员，成长值 128。
- 用户地址：公司地址“城市广场 A 座 1208”和家庭地址“湖畔花园 3 号楼 1801”。
- 商家：基础商家账号和 13 个分业务商家账号。
- 管理员：`demo_admin`。

### 8.2 门店与商品

初始门店覆盖八类业务：

| 门店 | 类型 | 示例商品 / 服务 |
| --- | --- | --- |
| 塔斯汀中国汉堡 | 外卖 | 藤椒鸡腿堡、黑椒牛肉堡、双人汉堡套餐 |
| 松记炸鸡饭 | 外卖 | 招牌炸鸡饭、鸡排饭双拼套餐 |
| 米村拌饭 | 外卖 | 招牌石锅拌饭、肥牛泡菜拌饭、双人拌饭套餐 |
| 江南小馆 | 团购 | 3-4 人餐、双人餐 |
| 琥珀烤肉 | 团购 | 烤肉双人餐、家庭 4 人餐 |
| 云栖酒店 | 酒店 | 舒适大床房券 |
| 曼居影院酒店 | 酒店 | 影音大床房券、商旅双床房券 |
| 星盒密室 | 休闲娱乐 | 4 人套票 |
| 趣动电玩城 | 休闲娱乐 | 120 币套餐、VR 双人畅玩票 |
| 光影剧场 | 电影演出 | 电影通兑票 |
| 轻颜护理 | 丽人医美 | 基础皮肤护理 |
| 悦己SPA | 丽人医美 | 全身舒缓 SPA、肩颈放松 |
| 城市观景 | 景点门票 | 成人票 |
| 雅境足道 | 洗脚按摩 | 经典足疗、肩颈舒缓 |

### 8.3 业务闭环数据

seed 同时初始化以下演示数据：

- 商品分类、商品 SKU、推荐位；
- 外卖配送规则、商家接单模式、客服自动回复规则；
- 用户订单、订单明细、模拟支付记录；
- 券码、二维码 payload、核销状态；
- 外卖配送任务和配送轨迹节点；
- 预约记录；
- 评价、商家回复、评价点赞、评价举报和审核日志；
- 用户客服会话和消息；
- 投诉工单和处理日志；
- 用户收藏、站内消息、平台公告；
- 系统配置、会员等级、优惠券模板、用户优惠券；
- 系统审计日志。

## 9. 生产环境启动教程

### 9.1 生产部署总览

生产服务器 `aituan-new` 当前推荐使用 Kubernetes / k3s：

```text
GitHub Actions
  -> 测试后端、Web、Flutter、E2E
  -> 构建 backend / web 镜像
  -> 推送 GHCR：ghcr.io/puaa-team/aituan/backend:sha-xxxxxxx
              ghcr.io/puaa-team/aituan/web:sha-xxxxxxx
  -> 写入或复用 K8s Secret
  -> apply k8s manifests
  -> set image 到本次 sha tag
  -> rollout status 等待发布完成
  -> 检查 /actuator/health
```

Docker Compose 用于回退部署或没有 k3s 时的部署。注意：同一台服务器上不要同时让 K8s Web Service 和 Docker Compose Nginx 占用 80/443。

### 9.2 Kubernetes 首次启动

以下命令在 `aituan-new` 上执行。真实 secret 值需要在服务器上手动创建，不能提交到仓库。

1. 登录服务器并进入部署目录：

```bash
ssh aituan-new
cd /opt/aituan/app
```

2. 创建命名空间：

```bash
kubectl apply -f k8s/00-namespace.yaml
```

3. 创建数据库 Secret。把示例值替换成真实值：

```bash
kubectl -n aituan create secret generic aituan-db-secret \
  --from-literal=MYSQL_USER='<真实 MySQL 用户>' \
  --from-literal=MYSQL_PASSWORD='<真实 MySQL 密码>' \
  --from-literal=MYSQL_ROOT_PASSWORD='<真实 MySQL root 密码>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

4. 创建后端 `.config` Secret。先在服务器临时写入配置文件，再生成 Secret，最后删除临时文件：

```bash
cat > /tmp/aituan-app.config <<'EOF'
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.mail.enabled=false
aituan.mail.debug-return-code=false
aituan.ai.enabled=false
aituan.upload.strategy=local
aituan.map.provider=local
EOF

kubectl -n aituan create secret generic aituan-app-config \
  --from-file=.config=/tmp/aituan-app.config \
  --dry-run=client -o yaml | kubectl apply -f -
rm -f /tmp/aituan-app.config
```

5. 如果 GHCR 镜像是私有包，创建镜像拉取 Secret；如果镜像公开，可以跳过：

```bash
kubectl -n aituan create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username='<GitHub 用户名或机器人账号>' \
  --docker-password='<具备 read:packages 权限的 PAT>' \
  --dry-run=client -o yaml | kubectl apply -f -
```

6. 创建 TLS Secret。证书文件来自服务器 `/etc/letsencrypt/live/aituan.2b.gs/`：

```bash
kubectl -n aituan create secret tls aituan-tls \
  --cert=/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem \
  --key=/etc/letsencrypt/live/aituan.2b.gs/privkey.pem \
  --dry-run=client -o yaml | kubectl apply -f -
```

7. 应用资源：

```bash
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-mysql.yaml
kubectl apply -f k8s/03-backend.yaml
kubectl apply -f k8s/04-web.yaml
kubectl apply -f k8s/05-ingress.yaml
```

8. 指定实际镜像 tag。把 `sha-a7e2145` 换成本次 GitHub Actions 生成的 tag：

```bash
kubectl -n aituan set image deployment/aituan-backend backend=ghcr.io/puaa-team/aituan/backend:sha-a7e2145
kubectl -n aituan set image deployment/aituan-web web=ghcr.io/puaa-team/aituan/web:sha-a7e2145
```

9. 等待发布完成并检查：

```bash
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
kubectl -n aituan get pods,svc,ingress
curl -fsS http://127.0.0.1/actuator/health
```

### 9.3 Kubernetes 日常启动、停止、重启和排查

查看状态：

```bash
ssh aituan-new "kubectl -n aituan get pods,svc,ingress,deploy,statefulset"
```

重启后端：

```bash
ssh aituan-new "kubectl -n aituan rollout restart deployment/aituan-backend && kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s"
```

重启 Web：

```bash
ssh aituan-new "kubectl -n aituan rollout restart deployment/aituan-web && kubectl -n aituan rollout status deployment/aituan-web --timeout=300s"
```

更新镜像：

```bash
ssh aituan-new "kubectl -n aituan set image deployment/aituan-backend backend=ghcr.io/puaa-team/aituan/backend:sha-xxxxxxx"
ssh aituan-new "kubectl -n aituan set image deployment/aituan-web web=ghcr.io/puaa-team/aituan/web:sha-xxxxxxx"
```

查看日志：

```bash
ssh aituan-new "kubectl -n aituan logs deployment/aituan-backend --tail=200"
ssh aituan-new "kubectl -n aituan logs deployment/aituan-web --tail=200"
ssh aituan-new "kubectl -n aituan describe pod -l app.kubernetes.io/name=aituan-backend"
```

回滚到上一个 ReplicaSet：

```bash
ssh aituan-new "kubectl -n aituan rollout undo deployment/aituan-backend"
ssh aituan-new "kubectl -n aituan rollout undo deployment/aituan-web"
```

临时停止业务副本：

```bash
ssh aituan-new "kubectl -n aituan scale deployment/aituan-backend --replicas=0"
ssh aituan-new "kubectl -n aituan scale deployment/aituan-web --replicas=0"
```

恢复业务副本：

```bash
ssh aituan-new "kubectl -n aituan scale deployment/aituan-backend --replicas=1"
ssh aituan-new "kubectl -n aituan scale deployment/aituan-web --replicas=1"
```

数据库通常不要随意缩容或删除 PVC。MySQL StatefulSet 的数据在 PVC 中保存，删除 PVC 会丢失数据库数据。

### 9.4 Docker Compose 生产回退启动

Docker Compose 回退方案适合暂时不用 k3s 的情况。执行前先确认 K8s 没有占用 80/443，或先停掉 K8s Web：

```bash
ssh aituan-new "kubectl -n aituan scale deployment/aituan-web --replicas=0"
```

1. 登录服务器并进入部署目录：

```bash
ssh aituan-new
cd /opt/aituan/app
```

2. 准备 `deploy/.env`。不要把真实 `.env` 提交到 Git：

```bash
cat > deploy/.env <<'EOF'
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
EOF
```

3. 准备后端 `.config`。不要把真实 `.config` 提交到 Git：

```bash
cat > .config <<'EOF'
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.mail.enabled=false
aituan.mail.debug-return-code=false
aituan.ai.enabled=false
aituan.upload.strategy=local
aituan.map.provider=local
EOF
```

4. 创建数据和下载目录：

```bash
mkdir -p /opt/aituan/data/mysql /opt/aituan/data/uploads /opt/aituan/data/downloads /var/www/certbot/.well-known/acme-challenge /etc/letsencrypt
```

5. 启动 CI/CD 镜像化 Compose：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
```

6. 检查状态和健康：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml ps
curl -fsS http://127.0.0.1/actuator/health
```

7. 查看日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs --tail=120 backend
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs --tail=120 nginx
```

停止 Compose：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml down
```

如果只是首次申请证书前的 HTTP 临时站点，可用 ACME Compose，仅占用 80：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.acme.yml up -d
```

如果使用服务器本地静态产物和本地构建镜像，可用 `deploy/docker-compose.server.yml`：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

### 9.5 GitHub Actions 配置和发布

仓库有三个主要 workflow：

| Workflow | 触发方式 | 作用 |
| --- | --- | --- |
| `aituan-ci` | Pull Request / 手动触发 | 静态回归、后端测试、MySQL 迁移 smoke、Web 测试构建、Flutter 测试构建、E2E |
| `aituan-deploy` | push main / 手动触发 | 测试、构建镜像、推送 GHCR，并按 k8s / compose / none 发布 |
| `aituan-android-apk` | 手动触发 | 构建用户端 Android debug APK，可选上传到服务器下载目录 |

生产环境 Variables 建议：

| Variable | 建议值 | 说明 |
| --- | --- | --- |
| `SERVER_ORIGIN` | `https://aituan.2b.gs` | 站点 origin，不要带 `/api` |
| `AUTO_DEPLOY_PRODUCTION` | `true` | main push 是否自动生产部署 |
| `DEPLOY_TARGET` | `k8s` | 默认生产部署目标，可选 `k8s`、`compose`、`none` |
| `K8S_NAMESPACE` | `aituan` | Kubernetes 命名空间 |
| `SERVER_APP_DIR` | `/opt/aituan/app` | Compose / APK 上传使用的服务器目录 |

Kubernetes 发布需要的 Production Secrets：

| Secret | 用途 |
| --- | --- |
| `KUBE_CONFIG` | 连接 `aituan-new` k3s 集群的 kubeconfig 内容 |
| `K8S_MYSQL_USER` | 创建或更新 `aituan-db-secret` 的 MySQL 用户 |
| `K8S_MYSQL_PASSWORD` | 创建或更新 `aituan-db-secret` 的 MySQL 密码 |
| `K8S_MYSQL_ROOT_PASSWORD` | 创建或更新 `aituan-db-secret` 的 root 密码 |
| `K8S_APP_CONFIG` | 创建或更新 `aituan-app-config` 的完整 `.config` 内容 |
| `GHCR_PULL_USERNAME` | 私有 GHCR 镜像拉取账号，公开镜像可不填 |
| `GHCR_PULL_TOKEN` | 私有 GHCR 镜像拉取 token，公开镜像可不填 |

Compose 发布和 APK 上传还需要：

| Secret | 用途 |
| --- | --- |
| `SERVER_HOST` | 服务器 IP 或域名 |
| `SERVER_PORT` | SSH 端口，默认 22 |
| `SERVER_USER` | SSH 用户 |
| `SERVER_SSH_KEY` | GitHub Actions 连接服务器的私钥 |
| `SERVER_KNOWN_HOSTS` | known_hosts 内容，避免 SSH 交互确认 |

手动触发 `aituan-deploy`：

1. 打开 GitHub Actions；
2. 选择 `aituan-deploy`；
3. 点击 `Run workflow`；
4. `deploy=true`；
5. `deploy_target=k8s` 表示部署到当前生产 k3s；
6. `deploy_target=compose` 表示走 Docker Compose 回退部署；
7. `deploy_target=none` 表示只测试、构建、推送镜像，不发布。

发布后在服务器检查：

```bash
ssh aituan-new "kubectl -n aituan get pods,svc,ingress,deploy,statefulset"
ssh aituan-new "curl -fsS http://127.0.0.1/actuator/health"
```

### 9.6 GitHub Actions 构建 APK 并上传服务器

手动触发 `aituan-android-apk`：

| 输入 | 建议值 | 说明 |
| --- | --- | --- |
| `api_origin` | 留空或 `https://aituan.2b.gs` | 留空时使用 `SERVER_ORIGIN` |
| `upload_to_server` | `true` | 是否上传到服务器下载目录 |
| `apk_name` | `aituan-user-server-debug.apk` 或留默认 | 默认会按版本号生成 `aituan-user-版本-server-debug.apk` |

流程会执行：

1. 设置 Java 17；
2. 设置 Flutter 3.41.6；
3. 校验 API origin；
4. `flutter pub get`；
5. `flutter analyze`；
6. `flutter test`；
7. `flutter build apk --debug --dart-define=API_BASE_URL=<生产域名>`；
8. 上传 APK artifact；
9. 如果 `upload_to_server=true`，通过 SSH 上传到 `/opt/aituan/data/downloads` 或 `deploy/.env` 中的 `AITUAN_DOWNLOADS_DIR`。

生产下载入口：

```text
https://aituan.2b.gs/downloads/<APK 文件名>
```

### 9.7 生产发布失败处理

1. 先看 GitHub Actions 的失败 job 和上传的原始报告 artifact。
2. 如果失败发生在 K8s rollout：

```bash
ssh aituan-new "kubectl -n aituan get pods,svc,ingress,events"
ssh aituan-new "kubectl -n aituan describe deployment/aituan-backend"
ssh aituan-new "kubectl -n aituan logs deployment/aituan-backend --tail=200"
```

3. 如果新镜像启动失败，回滚：

```bash
ssh aituan-new "kubectl -n aituan rollout undo deployment/aituan-backend"
ssh aituan-new "kubectl -n aituan rollout undo deployment/aituan-web"
```

4. 如果 Compose 发布失败：

```bash
ssh aituan-new "cd /opt/aituan/app && docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml ps"
ssh aituan-new "cd /opt/aituan/app && docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs --tail=200 backend"
```

5. 数据库结构问题统一通过新的 Flyway 迁移解决。已执行过的迁移版本不要直接改内容。

## 10. 配置要点

### 10.1 后端 `.config`

后端默认读取 `.config`。本地没有该文件也能以 demo 默认值启动；生产必须设置强随机 JWT secret。

```properties
aituan.security.jwt-secret=<生产强随机 JWT secret>
aituan.ai.enabled=false
aituan.ai.api-url=http://cliapi.2b.gs
aituan.ai.api-key=
aituan.ai.model=pp/gpt-5.5
aituan.mail.enabled=false
aituan.mail.debug-return-code=false
aituan.upload.strategy=local
aituan.upload.root-dir=/data/uploads
aituan.map.provider=local
```

真实数据库密码、邮箱授权码、AI key、图床 token、SSH 私钥、kubeconfig 和证书私钥不要提交到仓库。

### 10.2 Docker Compose `.env`

Compose 生产回退部署使用 `deploy/.env`：

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

### 10.3 Kubernetes 配置

Kubernetes 固定命名空间为 `aituan`。核心配置：

```yaml
MYSQL_DATABASE: aituan_dev
SPRING_PROFILES_ACTIVE: dev
SERVER_PORT: "8080"
AITUAN_UPLOAD_ROOT: /data/uploads
AITUAN_UPLOAD_PUBLIC_PREFIX: /api/common/files
MANAGEMENT_HEALTH_MAIL_ENABLED: "false"
JAVA_TOOL_OPTIONS: "-Xms128m -Xmx512m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
TZ: Asia/Shanghai
NGINX_SERVER_NAME: aituan.2b.gs
```

Kubernetes 资源关系：

```text
mysql StatefulSet
  -> mysql Service: 3306
backend Deployment
  -> backend Service: 8080
web Deployment
  -> web LoadBalancer Service: 80 / 443
  -> /api/ 反向代理 backend:8080
  -> /actuator/health 反向代理 backend:8080/actuator/health
```

### 10.4 数据与证书

- MySQL 数据：K8s 使用 `mysql-data` PVC；Compose 使用 `/opt/aituan/data/mysql`。
- 上传文件：K8s 使用 `aituan-uploads` PVC，挂载到 `/data/uploads`；Compose 使用 `/opt/aituan/data/uploads`。
- APK 下载：K8s 使用 `aituan-downloads` PVC，挂载到 `/usr/share/nginx/html/downloads`；Compose 使用 `/opt/aituan/data/downloads`。
- TLS 证书：服务器路径为 `/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem` 和 `/etc/letsencrypt/live/aituan.2b.gs/privkey.pem`；K8s 中同步为 `aituan-tls` Secret。
- 迁移脚本必须兼容 MySQL 8 和 H2 `MODE=MySQL`，生产数据库结构变更统一走 Flyway。
