# 爱团本地生活服务平台

爱团是一个面向本地生活场景的综合服务平台，覆盖外卖点餐、到店团购、酒店、休闲娱乐、电影演出、丽人医美、景点门票、足疗按摩等业务。项目包含 Flutter 用户端 APP、Spring Boot 后端服务、Flyway 数据库迁移脚本和本地构建部署脚本，目标是提供一套可运行、可联调、可扩展的本地生活服务系统。

## 功能概览

- 用户认证：邮箱验证码、注册、密码登录、找回密码、Token 校验。
- 首页发现：服务模块、猜你喜欢、热门商家与商品推荐。
- 商家搜索：按商家、商品、标签和业务类型检索本地服务。
- 外卖点单：商家详情、分类商品、购物车、确认订单、模拟支付、配送履约。
- 到店服务：商品/服务详情、到店订单、券码和二维码核销凭证。
- 订单中心：未支付、待完成、未使用、已使用等订单状态展示。
- 个人中心：用户资料、消息、收藏、地址和会员信息入口。
- 后台接口：预留商家/运营侧订单推进、券码核销和配送状态推进接口。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 用户端 APP | Flutter / Dart |
| 后端服务 | Java 17 / Spring Boot 3 / Maven |
| 数据库 | MySQL 8，演示环境可使用 H2 内存库 |
| 数据库迁移 | Flyway |
| 接口文档 | springdoc-openapi / Swagger UI |
| 构建脚本 | PowerShell |

## 目录结构

```text
.
├─ apps/user_app/                 # Flutter 用户端 APP
├─ services/backend/              # Spring Boot 后端服务
├─ database/migrations/           # Flyway 增量迁移脚本
├─ database/seeds/                # 幂等演示数据脚本
├─ scripts/build/                 # 构建脚本
├─ scripts/dev/                   # 本地启动脚本
├─ scripts/release/               # 构建产物清理脚本
└─ docs/                          # 设计、阶段说明和联调文档
```

## 环境要求

### 必需环境

- JDK 17+
- Maven 3.9+
- Flutter SDK
- Android Studio 或 Android SDK（构建 Android APK 时需要）
- MySQL 8.x（使用 `dev` profile 时需要）
- PowerShell（使用仓库脚本时需要）

### 本地端口

| 服务 | 默认端口 |
| --- | --- |
| 后端 API | `8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| 健康检查 | `http://localhost:8080/actuator/health` |

## 快速启动：后端 Demo 环境

Demo 环境使用 H2 内存数据库，适合首次运行、接口体验和 APP 联调，不需要提前安装 MySQL。

### 1. 克隆仓库

```bash
git clone https://github.com/PUAA-Team/aituan.git
cd aituan
```

### 2. 构建后端

通用方式：

```bash
mvn -f services/backend/pom.xml clean package
```

构建完成后，jar 位于：

```text
services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

Windows 本地也可以使用项目脚本：

```powershell
.\scripts\build\build_backend.ps1
```

该脚本会把构建缓存和最终 jar 放到 D 盘约定目录：

```text
D:/aituan_cache/m2/
D:/aituan_release/backend/aituan-backend.jar
```

> 如果你的 JDK 不在 `D:/tools/jdk-17.0.18+8`，请先修改脚本中的 `$JavaHome`，或使用上面的 Maven 通用命令。

### 3. 启动后端

通用方式：

```bash
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

默认 profile 是 `demo`，会自动使用 H2 内存数据库并执行 Flyway 迁移和演示数据。

Windows 脚本方式：

```powershell
.\scripts\dev\start_backend.ps1
```

脚本会启动：

```text
D:/aituan_release/backend/aituan-backend.jar
```

并将日志写入：

```text
D:/aituan_runtime/backend/backend.log
```

### 4. 验证后端

启动成功后访问：

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui.html
```

常用演示账号（仅用于本地开发、课程验收和演示；公网/生产部署前应禁用、删除或修改默认账号密码）：

| 角色/业务 | 登录名 | 邮箱或手机号 | 密码 |
| --- | --- | --- | --- |
| 用户 | `demo_user` | `user@example.com` 或 `18800001111` | `123456` |
| 默认商家 | `demo_merchant` | `merchant@example.com` 或 `18800002222` | `123456` |
| 后台运营 | `demo_admin` | `admin@example.com` 或 `18800003333` | `123456` |
| 外卖商家 | `demo_takeaway_merchant` | `takeaway@example.com` 或 `18800002021` | `123456` |
| 团购商家 | `demo_groupbuy_merchant` | `groupbuy@example.com` 或 `18800002022` | `123456` |
| 酒店商家 | `demo_hotel_merchant` | `hotel@example.com` 或 `18800002023` | `123456` |
| 休闲娱乐商家 | `demo_entertainment_merchant` | `entertainment@example.com` 或 `18800002024` | `123456` |
| 电影演出商家 | `demo_movie_merchant` | `movie@example.com` 或 `18800002025` | `123456` |
| 丽人医美商家 | `demo_beauty_merchant` | `beauty@example.com` 或 `18800002026` | `123456` |
| 景点门票商家 | `demo_ticket_merchant` | `ticket@example.com` 或 `18800002027` | `123456` |
| 洗脚按摩商家 | `demo_massage_merchant` | `massage@example.com` 或 `18800002028` | `123456` |
| 拌饭外卖商家 | `demo_bibimbap_merchant` | `bibimbap@example.com` 或 `18800002029` | `123456` |
| 烧烤商家 | `demo_bbq_merchant` | `bbq@example.com` 或 `18800002030` | `123456` |
| 酒店房型商家 | `demo_hotel_room_merchant` | `hotelroom@example.com` 或 `18800002031` | `123456` |
| 电玩城商家 | `demo_arcade_merchant` | `arcade@example.com` 或 `18800002032` | `123456` |
| SPA 商家 | `demo_spa_merchant` | `spa@example.com` 或 `18800002033` | `123456` |

## 使用 MySQL 部署后端

生产或长期运行建议使用 MySQL，并使用环境变量注入数据库连接和 JWT 密钥。

### 1. 创建数据库和账号

```sql
CREATE DATABASE aituan_dev
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'aituan'@'%' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON aituan_dev.* TO 'aituan'@'%';
FLUSH PRIVILEGES;
```

### 2. 配置环境变量

Linux/macOS：

```bash
export SPRING_PROFILES_ACTIVE=dev
export AITUAN_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/aituan_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
export AITUAN_DATASOURCE_USERNAME='aituan'
export AITUAN_DATASOURCE_PASSWORD='your_password_here'
export AITUAN_JWT_SECRET='replace-with-a-long-random-secret'
```

Windows PowerShell：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:AITUAN_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/aituan_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:AITUAN_DATASOURCE_USERNAME = 'aituan'
$env:AITUAN_DATASOURCE_PASSWORD = 'your_password_here'
$env:AITUAN_JWT_SECRET = 'replace-with-a-long-random-secret'
```

不要把真实数据库密码、JWT secret、邮箱授权码等敏感信息提交到 Git。

### 3. 启动服务

```bash
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

Flyway 会在启动时自动执行：

```text
database/migrations/
database/seeds/
```

迁移策略：

- 只使用 Flyway 增量迁移更新结构。
- `clean` 已禁用，避免清空数据库。
- 演示数据脚本要求幂等，重复执行不应清空真实数据。

## Flutter 用户端运行

### 1. 获取依赖

```bash
cd apps/user_app
flutter pub get
```

### 2. 连接本机后端运行

Android 模拟器访问宿主机后端时使用 `10.0.2.2`：

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

桌面或浏览器调试可使用：

```bash
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

真机调试时，需要把 `API_BASE_URL` 改成电脑在局域网中的 IP，例如：

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.100:8080
```

### 3. 构建 Android Debug APK

在项目根目录运行：

```powershell
.\scripts\build\build_android_apk.ps1
```

脚本会执行：

1. 同步 `apps/user_app/` 到 D 盘构建目录。
2. 设置 Flutter Pub 缓存到 `D:/aituan_cache/pub/`。
3. 设置 Gradle 缓存到 `D:/aituan_cache/gradle/`。
4. 执行 `flutter pub get`。
5. 执行 `flutter analyze`。
6. 执行 `flutter test`。
7. 执行 `flutter build apk --debug`。
8. 输出 APK 到：

```text
D:/aituan_release/apk/aituan-user-debug.apk
```

9. 删除临时构建目录，只保留最终 APK。

如果手动构建：

```bash
cd apps/user_app
flutter build apk --debug --dart-define=API_BASE_URL=http://your-backend-host:8080
```

### 4. 合并后保持 Web 与 Android App 版本一致

用户端 Web 和 Android App 共用 `apps/user_app/` 下同一套 Flutter 代码，但它们是两个不同的构建产物。合并分支后必须用同一个 Git commit 分别重新构建 Web 和 APK，不能只更新 Web 页面。

版本号统一维护在 `apps/user_app/pubspec.yaml` 的 `version` 字段。Android `versionName/versionCode` 会自动读取该字段；关于页会显示 `v版本号+构建号` 和构建 commit，验收时 Web 与 App 两端显示的 commit 应一致。

推荐合并后执行：

```bash
git pull
cd apps/user_app
flutter pub get
flutter build web --base-href /web/ \
  --dart-define=API_BASE_URL=http://your-backend-host:8080 \
  --dart-define=AITUAN_BUILD_COMMIT=$(git rev-parse HEAD) \
  --dart-define=AITUAN_BUILD_SOURCE=manual
flutter build apk --debug \
  --dart-define=API_BASE_URL=http://your-backend-host:8080 \
  --dart-define=AITUAN_BUILD_COMMIT=$(git rev-parse HEAD) \
  --dart-define=AITUAN_BUILD_SOURCE=manual
```

Android 模拟器本地联调仍使用：

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

## 常用接口验证

### 登录

```bash
curl -X POST http://localhost:8080/api/open/auth/user/login/password \
  -H "Content-Type: application/json" \
  -d '{"account":"user@example.com","password":"123456"}'
```

### 首页

```bash
curl http://localhost:8080/api/app/discovery/home
```

### 搜索商家

```bash
curl "http://localhost:8080/api/app/discovery/stores/search?keyword=汉堡&page=1&pageSize=12"
```

### 订单列表

需要先登录并在请求中带上 Token：

```bash
curl http://localhost:8080/api/app/trade/orders \
  -H "Authorization: Bearer <your-token>"
```

## 清理构建产物

普通清理：

```powershell
.\scripts\release\clean_build_artifacts.ps1
```

连依赖缓存一起清理：

```powershell
.\scripts\release\clean_build_artifacts.ps1 -IncludeCache
```

默认不会删除 `D:/aituan_release/apk/` 下已经生成的 APK。

## 部署注意事项

1. 公开部署时必须在 `.config` 设置强随机 `aituan.security.jwt-secret`。
2. MySQL 用户请只授权当前业务库，不建议使用 root 账号运行应用。
3. 不要在仓库中提交真实数据库密码、JWT secret、邮箱授权码、`.config` 或生产配置文件。
4. 数据库结构更新只走 Flyway 增量迁移，不要手动清表重灌。
5. Android 模拟器、真机和桌面环境访问后端的地址不同，联调时优先检查 `API_BASE_URL`。
6. 如果使用仓库 PowerShell 脚本，请根据本机实际 JDK 路径调整 `$JavaHome`。

## 更多文档

- `docs/ReadMe.md`：项目文档索引。
- `docs/stage1/API 分组设计.md`：接口分组设计。
- `docs/stage1/数据库表设计.md`：数据库设计。
- `docs/stage3/APP打包说明.md`：APP 打包说明。
- `docs/stage4/后端部署与联调说明.md`：后端部署和 Flutter 联调说明。
