# 域名 HTTPS 证书部署说明

## 1. 当前目标

| 项目 | 当前值 |
| --- | --- |
| SSH 别名 | `aituan-new` |
| 服务器 IP | `8.220.192.106` |
| 域名 | `aituan.2b.gs` |
| 对外 Origin | `https://aituan.2b.gs` |
| 部署目录 | `/opt/aituan/app` |
| Docker Compose | `deploy/docker-compose.server.yml` / `deploy/docker-compose.cicd.yml` |
| Let's Encrypt 邮箱 | `aituan@zuoai.de` |

`SERVER_ORIGIN` 只写站点 Origin：`https://aituan.2b.gs`，不要写 `/api`，不要带末尾 `/`。

## 2. 前置条件

1. DNS A 记录已将 `aituan.2b.gs` 指向 `8.220.192.106`。
2. 云服务器安全组放行：
   - TCP `80`：HTTP、ACME HTTP-01 验证。
   - TCP `443`：HTTPS。
   - TCP `22`：SSH 部署。
3. 服务器本机防火墙如启用，也需要放行 `80/tcp` 和 `443/tcp`。
4. 服务器当前已有 HTTP 版 Docker 部署，并能访问：
   - `http://8.220.192.106/actuator/health`
   - `http://aituan.2b.gs/actuator/health`
5. 不要把真实 `.env`、`.config`、证书私钥、SSH 私钥、数据库密码、JWT secret 写入 Git 或文档。

## 3. 仓库中的 HTTPS 配置文件

| 文件 | 用途 |
| --- | --- |
| `deploy/nginx/conf.d/default.http.conf` | HTTP/ACME 临时配置，也可作为回滚配置。 |
| `deploy/nginx/conf.d/default.https.conf` | 服务器本地构建部署使用的正式 HTTPS Nginx 配置。 |
| `deploy/nginx/templates/default.conf.template` | CI/CD Web 镜像使用的 HTTPS Nginx 模板。 |
| `deploy/docker-compose.acme.yml` | 首次申请证书前的独立临时 Compose 文件，只暴露 80 并挂载 ACME webroot。 |
| `deploy/docker-compose.server.yml` | 手动/服务器本地构建部署，Nginx 暴露 80/443 并挂载证书目录。 |
| `deploy/docker-compose.cicd.yml` | GitHub Actions 镜像部署，Nginx 暴露 80/443 并挂载证书目录。 |

正式 HTTPS 配置保留 HTTP `/.well-known/acme-challenge/` 用于续期，并保留 HTTP `/actuator/health` 给 GitHub Actions 本机健康检查使用；其他 HTTP 请求跳转 HTTPS。

## 4. 首次申请证书流程

> 以下命令在服务器执行。执行前先备份当前 `/opt/aituan/app` 下的 compose 和 Nginx 配置。

### 4.1 准备目录

```bash
mkdir -p /var/www/certbot/.well-known/acme-challenge
mkdir -p /etc/letsencrypt
```

### 4.2 使用临时 ACME 配置启动 Nginx

证书尚不存在时，不要直接启用正式 HTTPS 配置，否则 Nginx 会因找不到 `fullchain.pem` / `privkey.pem` 启动失败。

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.acme.yml up -d nginx
```

验证 ACME 路径由当前 Nginx 响应：

```bash
printf 'ok' > /var/www/certbot/.well-known/acme-challenge/aituan-acme-test
curl -fsS http://aituan.2b.gs/.well-known/acme-challenge/aituan-acme-test
rm -f /var/www/certbot/.well-known/acme-challenge/aituan-acme-test
```

预期返回 `ok`。

### 4.3 申请 Let's Encrypt 证书

推荐使用一次性 certbot Docker 容器，避免修改宿主机软件环境：

```bash
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d aituan.2b.gs \
  --email aituan@zuoai.de \
  --agree-tos \
  --no-eff-email
```

申请成功后只检查文件名，不输出私钥内容：

```bash
ls -l /etc/letsencrypt/live/aituan.2b.gs/
```

应包含：

```text
fullchain.pem
privkey.pem
```

## 5. 切换正式 HTTPS

证书存在后再启用正式 HTTPS Compose：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d nginx
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml exec nginx nginx -t
```

如 `nginx -t` 通过，可继续验证公网 HTTPS。

## 6. 验证

```bash
curl -I http://aituan.2b.gs
curl -fsS http://127.0.0.1/actuator/health
curl -fsS https://aituan.2b.gs/actuator/health
curl -I https://aituan.2b.gs/
curl -I https://aituan.2b.gs/web/
curl -I https://aituan.2b.gs/merchant/
curl -I https://aituan.2b.gs/admin/
curl -I https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

浏览器 Network 中接口应请求：

```text
https://aituan.2b.gs/api/...
```

不应出现：

```text
http://8.220.192.106
http://182.92.238.178
http://43.108.39.91
localhost:8080
```

## 7. GitHub Actions 需要修改的配置

GitHub Variables：

```text
SERVER_ORIGIN=https://aituan.2b.gs
SERVER_APP_DIR=/opt/aituan/app
AUTO_DEPLOY_PRODUCTION=true 或 false
```

GitHub production Environment Secrets：

```text
SERVER_HOST=8.220.192.106
SERVER_PORT=22
SERVER_USER=<当前部署用户>
SERVER_SSH_KEY=<能登录 aituan-new 的私钥完整内容>
SERVER_KNOWN_HOSTS=<ssh-keyscan -H 8.220.192.106 输出>
```

说明：

- `SERVER_HOST` 是 SSH 连接目标，建议继续用 IP。
- `SERVER_ORIGIN` 是浏览器和 App 访问地址，HTTPS 后使用域名。
- 换服务器必须重新生成 `SERVER_KNOWN_HOSTS`，不能复用旧服务器输出。
- 如果 GHCR 镜像是私有包，服务器需要提前 `docker login ghcr.io`，或将对应 package 设为 public。

## 8. 重新构建 Web 与 APK

改完 `SERVER_ORIGIN` 后，必须重新运行：

1. `aituan-deploy`：重新构建后端/Web 镜像并部署。
2. `aituan-android-apk`：重新构建服务器版 APK。

本地手动构建时使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "https://aituan.2b.gs"
```

如重新交付用户端 APK，应按项目规则递增 `apps/user_app/pubspec.yaml` 版本号和 build number，并同步 `AppBuildInfo`。

## 9. 证书续期

Let's Encrypt 证书需要定期续期。可先做 dry-run：

```bash
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot renew --webroot -w /var/www/certbot --dry-run
```

正式续期成功后 reload Nginx：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml exec nginx nginx -s reload
```

本次服务器已配置 cron 自动续期：

```text
/etc/cron.d/aituan-certbot-renew
```

执行时间为每天 `03:17`，内容为运行 `certbot/certbot renew`，续期成功后 reload Nginx。`cron` 服务已确认 `enabled`、`active`。

脚本中不得打印证书私钥或服务器敏感配置。

## 10. 2026-08-26 实际结果

本次已在 `aituan-new` 完成 HTTPS 切换：

```text
200 https://aituan.2b.gs/
200 https://aituan.2b.gs/web/
200 https://aituan.2b.gs/merchant/
200 https://aituan.2b.gs/admin/
200 https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
200 https://aituan.2b.gs/actuator/health
```

证书摘要：

```text
subject=CN = aituan.2b.gs
issuer=Let's Encrypt
notAfter=Nov 24 05:41:53 2026 GMT
```

切换前备份目录：

```text
/opt/aituan/backups/ssl-migration-20260826-112038
```

HTTPS 产物重新部署备份目录：

```text
/opt/aituan/backups/https-artifacts-20260826-154628
```

旧 hash bundle 清理备份目录：

```text
/opt/aituan/backups/artifacts-clean-20260826-155139
```

## 11. 回滚

如果 HTTPS 切换失败，优先回滚 Nginx，不动 MySQL 和后端数据：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.acme.yml up -d nginx
```

或恢复备份中的 HTTP-only `docker-compose.server.yml` 与 `deploy/nginx/conf.d/default.http.conf` 后重启 Nginx。

回滚后验证：

```bash
curl -fsS http://8.220.192.106/actuator/health
curl -fsS http://aituan.2b.gs/actuator/health
```
