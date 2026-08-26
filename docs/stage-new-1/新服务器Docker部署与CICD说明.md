# 新服务器 Docker 部署与 CI/CD 说明

## 1. 服务器信息

- SSH 别名：`aituan-new`
- 系统：Debian 11
- 部署目录：`/opt/aituan/app`
- 数据目录：`/opt/aituan/data`
- 备份目录：`/opt/aituan/backups`
- 当前访问方式：HTTPS/域名（`https://aituan.2b.gs`），SSH/部署仍使用服务器 IP `8.220.192.106`

## 2. 已调整的部署形态

新服务器当前目标为域名 HTTPS 部署，公网入口由 Nginx 统一承载：

- `/`：入口页
- `/web/`：用户端 Web
- `/merchant/`：商家端
- `/admin/`：后台端
- `/api/`：后端 API
- `/actuator/health`：健康检查

CI/CD 镜像化部署使用：

```text
deploy/docker-compose.cicd.yml
```

本地服务器构建部署使用：

```text
deploy/docker-compose.server.yml
```

Nginx 配置文件：

```text
deploy/nginx/conf.d/default.http.conf      # 首次申请证书/回滚使用
deploy/nginx/conf.d/default.https.conf     # 服务器本地构建部署正式 HTTPS 配置
deploy/nginx/templates/default.conf.template # CI/CD Web 镜像使用的 HTTPS 模板
```

首次申请 Let's Encrypt 证书前使用 `deploy/docker-compose.acme.yml` 临时暴露 HTTP/ACME；证书存在后使用正式 HTTPS Compose。详细步骤见 `docs/stage-new-1/域名HTTPS证书部署说明.md`。

## 3. 服务器运行配置

服务器需要准备：

```text
/opt/aituan/app/deploy/.env
/opt/aituan/app/.config
```

`deploy/.env` 负责 Docker Compose、MySQL、镜像 tag 和数据目录配置。

`.config` 负责后端业务配置，如 JWT、邮箱、AI、上传和地图服务。

生产或公开演示环境不应提交真实 `.env`、`.config`、数据库密码、JWT secret、SMTP 授权码和第三方 Token。

## 4. GitHub Actions 配置

需要在 GitHub 仓库配置 Variables：

```text
SERVER_ORIGIN=https://aituan.2b.gs
SERVER_APP_DIR=/opt/aituan/app
AUTO_DEPLOY_PRODUCTION=true
```

需要在 GitHub 仓库配置 Secrets：

```text
SERVER_HOST=8.220.192.106
SERVER_PORT=22
SERVER_USER=root
SERVER_SSH_KEY=<用于登录 aituan-new 的私钥>
SERVER_KNOWN_HOSTS=<ssh-keyscan 8.220.192.106 的结果>
```

流水线逻辑：

1. 运行后端、Web、Flutter 测试与构建。
2. 构建后端镜像和 Web 镜像。
3. 推送到 GHCR。
4. SSH 到新服务器。
5. 更新 `deploy/.env` 中的镜像仓库和版本号。
6. 执行 `docker compose pull` 和 `docker compose up -d --remove-orphans`。
7. 通过 `http://127.0.0.1/actuator/health` 做健康检查。

## 5. 验证命令

服务器上可用：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs --tail=120 backend
curl -fsS http://127.0.0.1/actuator/health
```

外部访问：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
https://aituan.2b.gs/actuator/health
```

## 6. 2026-08-25 实际部署记录

本次已在 `aituan-new` 上完成 HTTP/IP 版 Docker 部署，当前使用服务器本地构建形态：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

远程备份路径：

```text
/opt/aituan/backups/manual-before-http-deploy-20260825-150958
```

已部署服务：

- `aituan-mysql`：`mysql:8.0`，仅 Compose 内网访问，健康状态正常。
- `aituan-backend`：`aituan-backend:server`，由 `deploy/artifacts/backend/aituan-backend.jar` 构建。
- `aituan-nginx`：`nginx:1.27-alpine`，公网暴露 `80:80`。

本次构建与部署产物：

```text
D:/aituan_release/apk/aituan-user-1.1.6-23-server-debug.apk
D:/aituan_release/apk/aituan-user-server-debug.apk
/opt/aituan/app/deploy/artifacts/downloads/aituan-user-1.1.6-23-server-debug.apk
/opt/aituan/app/deploy/artifacts/downloads/aituan-user-server-debug.apk
```

2026-08-25 外部 HTTP/IP 访问验证均已通过：

```text
http://8.220.192.106/
http://8.220.192.106/favicon.png
http://8.220.192.106/web/
http://8.220.192.106/merchant/
http://8.220.192.106/admin/
http://8.220.192.106/downloads/aituan-user-server-debug.apk
http://8.220.192.106/actuator/health
```

当日 `deploy/artifacts` 已确认不包含旧服务器 IP `182.92.238.178`；用户端 Web、商家端、后台端产物均使用 `http://8.220.192.106`。切换 HTTPS 后需要重新构建，当前目标 Origin 为 `https://aituan.2b.gs`。

本次修复的部署细节：

- 2026-08-25：`scripts/build/build_*_server.ps1` 默认 `ServerOrigin` 曾改为 `http://8.220.192.106`；2026-08-26 起已切换为 `https://aituan.2b.gs`。
- `build_frontends_server.ps1` 在重建 merchant/admin Web 前会清空输出目录，避免旧 hash 静态资源残留。
- `build_android_apk_server.ps1` 同时输出版本化 APK 和稳定下载名 APK。
- `deploy/web/Dockerfile.dockerignore` 已放行 Nginx HTTP 模板，避免 CI/CD 构建 web 镜像时缺文件。
- Nginx HTTP 配置已修复 `/web/`、`/merchant/`、`/admin/` 的 SPA fallback，避免目录索引 403/404。

## 7. 2026-08-26 HTTPS 切换记录

已完成：

- `aituan.2b.gs` 已解析到 `8.220.192.106`。
- 已使用 Let's Encrypt 申请证书，通知邮箱为 `aituan@zuoai.de`。
- 证书路径：
  - `/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem`
  - `/etc/letsencrypt/live/aituan.2b.gs/privkey.pem`
- 证书到期时间：`2026-11-24`。
- 远端 HTTPS 切换前备份目录：`/opt/aituan/backups/ssl-migration-20260826-112038`。
- `aituan-nginx` 已监听 `80:80` 和 `443:443`。
- 已配置自动续期任务 `/etc/cron.d/aituan-certbot-renew`，每天 `03:17` 检查续期，续期成功后 reload Nginx。
- `http://aituan.2b.gs/` 已 301 跳转到 `https://aituan.2b.gs/`。
- `http://127.0.0.1/actuator/health` 保留 HTTP 本机健康检查，兼容 GitHub Actions。

已验证通过：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
https://aituan.2b.gs/actuator/health
```

2026-08-26 已重新构建 HTTPS 产物，用户端 APK 版本为 `1.1.7+24`。远端旧 hash bundle 清理前备份目录：`/opt/aituan/backups/artifacts-clean-20260826-155139`。清理后远端 `deploy/artifacts` 已确认包含 `https://aituan.2b.gs`，且不再包含 `http://8.220.192.106`、`http://182.92.238.178`、`http://43.108.39.91`。

当前域名/证书切换步骤：

1. 域名 A 记录指向 `8.220.192.106`。
2. 服务器安全组放行 80/443。
3. 先用 `deploy/docker-compose.acme.yml` 启动 HTTP/ACME 临时 Nginx。
4. 使用 Let's Encrypt 为 `aituan.2b.gs` 申请证书，通知邮箱 `aituan@zuoai.de`。
5. 证书存在后切换正式 HTTPS Nginx 配置。
6. 将 GitHub Variable `SERVER_ORIGIN` 设为 `https://aituan.2b.gs`。
7. 重新触发 `aituan-deploy` 和 `aituan-android-apk`，避免 Web/APK 继续请求旧 HTTP/IP 地址。

完整命令和回滚方式见 `docs/stage-new-1/域名HTTPS证书部署说明.md`。
