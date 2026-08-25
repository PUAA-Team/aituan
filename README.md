# 爱团本地生活服务综合平台

爱团是一个面向本地生活服务场景的综合平台，覆盖用户消费、商家经营、平台治理和 AI 辅助。项目围绕“生活助手平台”课程题目设计，实现了用户端 APP / Web、商家端 Web、后台端 Web、Spring Boot 后端、MySQL 数据库、自动化测试和 CI/CD 部署链路。

项目目标不是只做页面原型，而是交付一套可以运行、可以联调、可以部署、可以测试、可以展示的课程级完整软件工程项目。

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
| 用户端 Web | Flutter Web 预览版，可由 Nginx 托管在 `/web/`。 |
| 商家端 Web | Vue 3 + TypeScript，商家经营控制台，可由 Nginx 托管在 `/merchant/`。 |
| 后台端 Web | Vue 3 + TypeScript，平台管理后台，可由 Nginx 托管在 `/admin/`。 |
| 后端服务 | Spring Boot 3 + Java 17，提供统一 REST API。 |
| 数据库 | MySQL 8，使用 Flyway 管理迁移和 seed 数据。 |
| 部署 | 支持 Docker Compose、常规 JAR + Nginx 安装、GitHub Actions + GHCR 镜像部署。 |
| 测试 | 后端 JUnit/MockMvc、Flutter test、Vitest、静态回归和 CI 自动化。 |

线上演示地址如已部署，可访问：

```text
https://aituan.2b.gs
```

## 2. 技术栈

| 层级 | 技术 |
| --- | --- |
| 用户端 | Flutter、Dart、Android APK、Flutter Web |
| 商家端 / 后台端 | Vue 3、TypeScript、Vite、Vitest、happy-dom |
| 后端 | Java 17、Spring Boot 3、Spring Security、JWT、JdbcTemplate、Flyway |
| 数据库 | MySQL 8；测试环境使用 H2 `MODE=MySQL` |
| 文件与资源 | 本地文件存储或外部图床配置，可通过 `.config` 切换 |
| AI 能力 | 后端 AI Assistant + Skills，支持外部模型调用和本地降级 |
| 部署 | Docker Compose、Nginx、GHCR、GitHub Actions、Let's Encrypt HTTPS |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、flutter_test、Vitest、Bash 静态回归 |

## 3. 目录结构

```text
.
├─ apps/
│  ├─ user_app/                 # Flutter 用户端 APP / Web
│  ├─ merchant_web/             # 商家端 Vue Web
│  └─ admin_web/                # 后台端 Vue Web
├─ services/
│  └─ backend/                  # Spring Boot 后端服务
├─ database/
│  ├─ migrations/               # Flyway 数据库迁移
│  └─ seeds/                    # 幂等演示数据
├─ deploy/                      # Docker Compose、Nginx、部署产物和示例配置
├─ scripts/
│  ├─ build/                    # 构建脚本
│  ├─ dev/                      # 本地启动脚本
│  └─ verify/                   # 静态回归验证脚本
├─ docs/                        # 需求、设计、测试、部署、阶段交付文档
└─ README.md                    # 项目总览
```

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

项目通过 `.config` 和 `deploy/.env` 区分业务配置与部署配置。真实密钥、数据库密码、JWT secret、AI key、邮箱授权码、图床 Token 不应提交到 Git。

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
AITUAN_IMAGE_TAG=<IMAGE_TAG>
AITUAN_DOWNLOADS_DIR=<DOWNLOADS_DIR>
```

## 6. 快速启动

### 6.1 后端 Demo 环境

Demo 环境适合本地开发和接口体验。默认会使用 H2 内存数据库，并自动执行 Flyway 迁移和演示数据。

```bash
mvn -B -f services/backend/pom.xml test
mvn -B -f services/backend/pom.xml -DskipTests package
java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

验证：

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui.html
```

### 6.2 MySQL 开发环境

创建数据库：

```sql
CREATE DATABASE <DB_NAME>
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER '<DB_USER>'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON <DB_NAME>.* TO '<DB_USER>'@'%';
FLUSH PRIVILEGES;
```

启动后端时配置环境变量：

```bash
export SPRING_PROFILES_ACTIVE=dev
export AITUAN_DATASOURCE_URL='jdbc:mysql://<DB_HOST>:<DB_PORT>/<DB_NAME>?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
export AITUAN_DATASOURCE_USERNAME='<DB_USER>'
export AITUAN_DATASOURCE_PASSWORD='<DB_PASSWORD>'
export AITUAN_CONFIG_FILE='<CONFIG_FILE>'

java -jar services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar
```

### 6.3 用户端 APP / Web

```bash
cd apps/user_app
flutter pub get
flutter analyze
flutter test
```

Android 模拟器访问宿主机后端时：

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

桌面或浏览器调试：

```bash
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

构建 Android Debug APK：

```bash
flutter build apk --debug --dart-define=API_BASE_URL=<PUBLIC_OR_LOCAL_ORIGIN>
```

也可以使用项目脚本：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build/build_android_apk.ps1
```

### 6.4 商家端 Web

```bash
npm ci --prefix apps/merchant_web
npm test --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

本地开发：

```bash
npm run dev --prefix apps/merchant_web
```

### 6.5 后台端 Web

```bash
npm ci --prefix apps/admin_web
npm test --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

本地开发：

```bash
npm run dev --prefix apps/admin_web
```

## 7. 测试

### 7.1 测试类型

| 端 / 层级 | 测试内容 | 工具 |
| --- | --- | --- |
| 后端 | 服务层集成测试、API 集成测试、权限边界测试 | JUnit 5、Spring Boot Test、MockMvc、H2、Flyway |
| 用户端 | 纯逻辑单测、Repository 测试、Widget 测试、静态分析 | flutter_test、flutter analyze |
| 商家端 Web | API 层测试、类型检查、构建校验 | Vitest、happy-dom、vue-tsc、Vite |
| 后台端 Web | API 层测试、类型检查、构建校验 | Vitest、happy-dom、vue-tsc、Vite |
| 跨端回归 | 关键入口、文案和代码形态检查 | Bash + grep |

### 7.2 常用测试命令

后端：

```bash
mvn -B -f services/backend/pom.xml test
```

用户端：

```bash
cd apps/user_app
flutter analyze
flutter test
```

商家端：

```bash
npm test --prefix apps/merchant_web
npm run build --prefix apps/merchant_web
```

后台端：

```bash
npm test --prefix apps/admin_web
npm run build --prefix apps/admin_web
```

静态回归：

```bash
bash scripts/verify/member_e_regression_checks.sh
```

### 7.3 当前测试覆盖

项目当前覆盖：

- 后端认证、权限、交易、优惠券、投诉、文件上传、评价、客服、会员成长值等高风险接口和服务。
- Flutter 输入校验、JSON 解析、业务枚举、金额计算、Repository 请求路径和部分 Widget。
- 商家端 / 后台端集中式 API 层。
- CI 中的 backend、web matrix、flutter 和 static-regression job。

详细报告见：

```text
docs/爱团测试报告.md
docs/stage7/全端测试体系总结.md
```

## 8. 部署

项目支持两种部署方式：

1. Docker Compose 部署；
2. 常规 JAR + MySQL + Nginx 安装。

详细部署说明见：

```text
docs/爱团通用部署文档.md
```

### 8.1 Docker Compose 部署

Compose 文件：

```text
deploy/docker-compose.server.yml
deploy/docker-compose.cicd.yml
```

手动产物部署通常使用：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

CI/CD 镜像化部署通常使用：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
```

### 8.2 HTTPS

Nginx 配置支持 Let's Encrypt 证书挂载：

```text
deploy/certbot/www
deploy/certbot/conf
```

HTTP 会跳转到 HTTPS，证书文件默认从容器内以下路径读取：

```text
/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem
/etc/letsencrypt/live/aituan.2b.gs/privkey.pem
```

如部署到其他域名，需要同步调整：

- `deploy/nginx/conf.d/default.conf` 中的 `server_name` 和证书路径；
- GitHub Actions Variable `SERVER_ORIGIN`；
- 前端构建时的 `API_BASE_URL` / `VITE_API_BASE_URL`。

### 8.3 常规安装

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
| `aituan-ci` | `.github/workflows/ci.yml` | PR / 手动触发，运行静态回归、后端测试、Web 测试构建、Flutter 测试构建。 |
| `aituan-deploy` | `.github/workflows/deploy.yml` | main / 手动触发，构建后端和 Web 镜像，推送 GHCR，并可部署服务器。 |
| `aituan-android-apk` | `.github/workflows/android-apk.yml` | 手动触发，构建用户端 Android debug APK，可选上传到服务器下载目录。 |

自动化部署通常需要配置：

- `SERVER_ORIGIN`：公开访问 origin，例如 `https://<DOMAIN>`，不要包含 `/api`；
- `SERVER_APP_DIR`：服务器部署目录；
- `SERVER_HOST`、`SERVER_PORT`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_KNOWN_HOSTS`：SSH 部署信息；
- 如镜像仓库为私有，还需要服务器具备读取镜像的权限。

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
- `docs/爱团测试报告.md`：项目测试报告。
- `docs/stage-final/期末展示PPT大纲.md`：期末展示 PPT 大纲。
- `docs/stage7/全端测试体系总结.md`：全端测试体系说明。
- `docs/stage6-memberE/AI助手交付说明.md`：AI 助手说明。
- `docs/stage1/API 分组设计.md`：API 分组设计。
- `docs/stage1/数据库表设计.md`：数据库设计。

## 12. 安全注意事项

1. 不要提交真实 `.config`、`deploy/.env`、数据库密码、JWT secret、AI key、邮箱授权码、SSH 私钥或第三方 Token。
2. 公开部署必须设置强随机 `aituan.security.jwt-secret`。
3. 邮箱验证码调试返回默认应关闭，不应在公开环境直接向前端返回验证码。
4. 服务器部署前建议备份 `.config`、`deploy/.env`、数据库和旧产物。
5. 数据库结构更新统一通过 Flyway 迁移，不建议手动改表后不留脚本。
6. Docker Compose 镜像部署建议使用 `sha-<短提交号>` 标签，便于追踪和回滚。
7. HTTPS 证书需要定期续期，续期后 reload Nginx。

## 13. 后续可扩展方向

- 接入真实支付沙箱和完整退款流程。
- 增加真实骑手端或更完整的配送轨迹模拟。
- 增加酒店房态、电影选座、技师排班等复杂预约能力。
- 引入 Playwright / Cypress 做浏览器端 E2E 测试。
- 引入 Flutter integration_test 做移动端端到端测试。
- 扩展 AI Skills、调用日志、智能推荐和运营辅助能力。
