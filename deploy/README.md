# 爱团服务器 Docker Compose 部署说明

本文说明后端 API、根路径下载展示页、用户端 Web、商家端 Web、后台端 Web 在 Debian 服务器上的 Compose 部署方式，以及如何构建对接服务器地址的前端产物。

## 1. 部署结构

服务器统一由 Nginx 暴露 80/443 端口，公网访问以 HTTPS 域名为准：

| 地址 | 说明 |
| --- | --- |
| `https://aituan.2b.gs/` | 用户端下载展示页 |
| `https://aituan.2b.gs/web/` | 用户端 Web 入口 |
| `https://aituan.2b.gs/merchant/` | 商家端 Web |
| `https://aituan.2b.gs/admin/` | 后台端 Web |
| `https://aituan.2b.gs/api/...` | 后端 API |
| `https://aituan.2b.gs/actuator/health` | 后端健康检查 |
| `https://aituan.2b.gs/downloads/aituan-user-server-debug.apk` | 用户端服务器版 APK 下载 |

Compose 服务包括：

- `mysql`：MySQL 8 数据库，不对公网暴露 3306。
- `backend`：Spring Boot 后端，仅在 Compose 内部网络暴露 8080。
- `nginx`：公网入口，托管根路径下载展示页、用户端 Web、商家端/后台端静态产物，并反向代理后端。

## 2. 敏感信息

不要把以下内容提交到 Git：

- SSH 密码、私钥。
- `deploy/.env`。
- `.config`。
- MySQL root 密码和业务用户密码。
- JWT secret。
- 证书私钥、邮箱授权码等密钥。

仓库只保留 `deploy/.env.example` 作为模板。真实服务器上需要复制并修改为 `deploy/.env`。

## 3. 本地构建服务器版产物

服务器版产物不会覆盖原开发版脚本和构建结果。

### 3.1 构建后端服务器版 JAR

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_backend_server.ps1"
```

输出：

```text
deploy/artifacts/backend/aituan-backend.jar
```

### 3.2 构建用户端、商家端和后台端服务器版 Web

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_frontends_server.ps1" -ServerOrigin "https://aituan.2b.gs"
```

输出：

```text
deploy/artifacts/landing
deploy/artifacts/user-web
deploy/artifacts/merchant-web
deploy/artifacts/admin-web
```

构建参数说明：

- 根路径下载展示页：`deploy/landing` 同步到 `/`
- 用户端 Web 构建 base：`/web/`，产物输出到 `deploy/artifacts/user-web`
- 商家端构建 base：`/merchant/`
- 后台端构建 base：`/admin/`
- API 地址：`https://aituan.2b.gs`

### 3.3 构建用户端服务器版 APK

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_android_apk_server.ps1" -ServerOrigin "https://aituan.2b.gs"
```

输出：

```text
D:/aituan_release/apk/aituan-user-server-debug.apk
deploy/artifacts/downloads/aituan-user-server-debug.apk
```

### 3.4 一次性构建全部服务器版产物

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "https://aituan.2b.gs"
```

如果只构建后端和两个 Web，不构建 APK：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "https://aituan.2b.gs" -SkipApk
```

## 4. 服务器目录建议

建议服务器上使用：

```text
/opt/aituan/app
/opt/aituan/data/mysql
/opt/aituan/data/uploads
/opt/aituan/backups
```

将仓库代码和 `deploy/artifacts` 产物上传到：

```text
/opt/aituan/app
```

## 5. 创建服务器配置文件

服务器部署使用两个不入仓库的配置文件：

- `deploy/.env`：仅供 Docker Compose 使用，保存 MySQL 容器账号、数据目录和 `.config` 挂载路径。
- `.config`：供 Spring Boot 后端读取，保存 JWT secret、邮箱 SMTP、地图 Key、图床等业务敏感配置。

在服务器中执行：

```bash
cd /opt/aituan/app/deploy
cp .env.example .env
cd /opt/aituan/app
cp .config.example .config
```

编辑 `deploy/.env`，把占位值改为真实值：

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=aituan
MYSQL_PASSWORD=替换为强密码
MYSQL_ROOT_PASSWORD=替换为强密码
AITUAN_DATA_DIR=/opt/aituan/data
AITUAN_CONFIG_HOST_FILE=../.config
AITUAN_NGINX_SERVER_NAME=aituan.2b.gs
AITUAN_LETSENCRYPT_DIR=/etc/letsencrypt
AITUAN_CERTBOT_WEBROOT=/var/www/certbot
```

编辑 `/opt/aituan/app/.config`，至少设置强随机 JWT secret；如需服务器发送 QQ 邮箱验证码，也在这里填写 SMTP 授权信息：

```properties
aituan.security.jwt-secret=替换为长随机字符串
aituan.mail.enabled=true
aituan.mail.debug-return-code=false
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=你的QQ邮箱
spring.mail.password=你的QQ邮箱授权码
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.starttls.enable=false
aituan.mail.from=你的QQ邮箱
aituan.mail.from-name=爱团
```

可用以下命令生成 JWT secret：

```bash
openssl rand -base64 48
```

## 6. 启动服务

首次启用 HTTPS 前需要先通过 `deploy/docker-compose.acme.yml` 启动 HTTP/ACME 临时配置并申请证书；完整步骤见 `docs/stage-new-1/域名HTTPS证书部署说明.md`。证书存在后，再使用下面的正式 Compose 命令启动服务。

在服务器中执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

查看状态：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml ps
```

查看日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml logs -f backend
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml logs -f nginx
```

## 7. 验证

### 7.0 演示账号

以下账号仅用于本地开发、课程验收和演示；公网/生产部署前应禁用、删除或修改默认账号密码。

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

### 7.1 后端健康检查

```bash
curl https://aituan.2b.gs/actuator/health
```

预期返回健康状态。

### 7.2 商家端

浏览器访问：

```text
https://aituan.2b.gs/merchant/
```

检查浏览器 Network：API 请求应为 `https://aituan.2b.gs/api/...`，不应出现 `localhost:8080`。

### 7.3 后台端

浏览器访问：

```text
https://aituan.2b.gs/admin/
```

检查浏览器 Network：API 请求应为 `https://aituan.2b.gs/api/...`。

### 7.4 用户端 APK

下载安装：

```text
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

安装后验证登录、首页、订单等接口能访问服务器。

## 8. 2C2G 服务器注意事项

- 不建议在服务器上运行 Maven、npm、Flutter 构建任务。
- MySQL 已按小内存场景限制连接数和缓冲池。
- 后端 JVM 默认限制为 `-Xmx512m`。
- 如遇 OOM，可先检查 `docker stats`，必要时增加 swap 或升级服务器配置。
- 云服务器安全组至少需要放行 80 和 443 端口；80 用于 ACME 验证和健康检查，443 用于 HTTPS。

## 9. 常用维护命令

停止服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml down
```

更新代码和产物后重启：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

查看资源占用：

```bash
docker stats
```
