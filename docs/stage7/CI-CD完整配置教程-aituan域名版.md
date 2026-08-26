# Stage7 CI/CD 完整配置教程（aituan.2b.gs 域名版）

> 本教程用于把当前爱团项目配置为 GitHub Actions + GHCR + Docker Compose 的自动化部署，并支持手动触发 Android APK 打包。本文使用服务器 IP `8.220.192.106`、访问域名 `aituan.2b.gs`。文档中不包含任何真实密码、SSH 私钥、GitHub Token、JWT secret 或数据库密码。

## 0. 当前目标

最终希望达到：

| 类型 | 地址 / 值 |
| --- | --- |
| 服务器 IP | `8.220.192.106` |
| 用户访问域名 | `aituan.2b.gs` |
| 对外 Origin | `https://aituan.2b.gs` |
| 后端健康检查 | `https://aituan.2b.gs/actuator/health` |
| 用户端 Web | `https://aituan.2b.gs/web/` |
| 商家端 Web | `https://aituan.2b.gs/merchant/` |
| 后台端 Web | `https://aituan.2b.gs/admin/` |
| APK 下载 | `https://aituan.2b.gs/downloads/aituan-user-server-debug.apk` |
| GHCR 镜像前缀 | `ghcr.io/puaa-team/aituan` |

说明：

- 当前教程按 HTTPS 域名配置，即 `https://aituan.2b.gs`。
- `SERVER_ORIGIN` 不要写 `/api`，不要写末尾 `/`。
- SSH 部署目标仍建议使用服务器 IP `8.220.192.106`，对外访问和前端/API Origin 使用域名。

## 1. 本地仓库已准备好的文件

当前仓库已经新增：

| 文件 | 作用 |
| --- | --- |
| `.github/workflows/ci.yml` | PR / 手动触发的后端、Web、Flutter 校验。 |
| `.github/workflows/deploy.yml` | 构建后端/Web 镜像，推送 GHCR，可控部署服务器。 |
| `.github/workflows/android-apk.yml` | 手动触发 Android debug APK 打包，可选上传服务器下载目录。 |
| `deploy/docker-compose.cicd.yml` | CI/CD 专用 Compose，服务器拉取 backend/web 镜像运行。 |
| `deploy/web/Dockerfile` | 构建 Nginx 静态 Web 镜像。 |
| `deploy/web/Dockerfile.dockerignore` | Web 镜像 build context 白名单。 |
| `deploy/.env.example` | 增加了 CI/CD 镜像变量模板。 |

当前 HTTPS 部署需要注意：

- `deploy/docker-compose.server.yml` 和 `deploy/docker-compose.cicd.yml` 需要暴露 80/443，并挂载证书目录与 ACME webroot。
- `deploy/nginx/templates/default.conf.template` 是 CI/CD Web 镜像内使用的 Nginx 模板。
- `deploy/nginx/conf.d/default.https.conf` 是服务器本地构建部署使用的正式 HTTPS 配置。
- 真实 `.config`、真实 `deploy/.env`、SSH 私钥、数据库密码、证书私钥不得提交。

## 2. DNS 域名解析配置

登录你的域名 DNS 控制台，为 `aituan.2b.gs` 增加 A 记录：

| 字段 | 值 |
| --- | --- |
| 主机记录 | `aituan` |
| 记录类型 | `A` |
| 记录值 | `8.220.192.106` |
| TTL | 默认即可 |

配置后在本机验证：

```powershell
nslookup aituan.2b.gs
```

预期能看到：

```text
8.220.192.106
```

如果 DNS 刚配置，可能需要等待几分钟到几十分钟。

## 3. 服务器安全组 / 防火墙

云服务器安全组至少放行：

| 端口 | 协议 | 用途 |
| --- | --- | --- |
| `22` | TCP | SSH 部署。 |
| `80` | TCP | HTTP 访问、Let's Encrypt HTTP-01 验证、CI/CD 本机健康检查。 |
| `443` | TCP | HTTPS 访问。 |

服务器本机如启用了防火墙，也需要允许 80 和 443：

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

如果没有使用 `ufw`，这条可以跳过。

## 4. GitHub 网页端配置

仓库地址：

```text
https://github.com/PUAA-Team/aituan
```

### 4.1 Actions 权限

进入：

```text
PUAA-Team/aituan -> Settings -> Actions -> General
```

检查：

- GitHub Actions 已启用。
- `Workflow permissions` 如果能选，建议选择 `Read and write permissions`。

如果 `Read and write permissions` 灰色不能选，通常是 `PUAA-Team` 组织级策略限制。先不用立刻卡住，因为 workflow 文件里已经声明：

```yaml
permissions:
  contents: read
  packages: write
```

可以先运行一次 `aituan-deploy` 验证。如果推 GHCR 镜像失败，再找组织 Owner 调整 Actions 权限。

### 4.2 创建 production Environment

进入：

```text
PUAA-Team/aituan -> Settings -> Environments -> New environment
```

新建：

```text
production
```

建议设置：

- Required reviewers：选择你自己或项目负责人。
- Deployment branches：限制为 `main`。

### 4.3 配置 GitHub Environment Secrets

进入：

```text
PUAA-Team/aituan -> Settings -> Environments -> production -> Environment secrets
```

新增：

| Secret | 值 |
| --- | --- |
| `SERVER_HOST` | `8.220.192.106` |
| `SERVER_PORT` | `22` |
| `SERVER_USER` | 服务器部署用户名，例如 `root` 或后续创建的专用部署用户 |
| `SERVER_SSH_KEY` | 部署专用 SSH 私钥完整内容 |
| `SERVER_KNOWN_HOSTS` | `ssh-keyscan -H 8.220.192.106` 的输出 |

注意：

- 不要把服务器密码放到 GitHub。
- 不要把私钥提交到仓库。
- 如果现在只能 root 登录，可以先用 root；后续建议创建部署专用用户。

### 4.4 配置 GitHub Variables

进入：

```text
PUAA-Team/aituan -> Settings -> Secrets and variables -> Actions -> Variables
```

新增：

| Variable | 值 |
| --- | --- |
| `SERVER_ORIGIN` | `https://aituan.2b.gs` |
| `SERVER_APP_DIR` | `/opt/aituan/app` |
| `AUTO_DEPLOY_PRODUCTION` | 可选；先不设置，或设置为 `false` |

说明：

- `SERVER_ORIGIN` 是前端和 APK 构建时写入的 API Origin。
- 当前使用 `https://aituan.2b.gs`。
- 修改该变量后必须重新跑部署和 APK workflow，否则旧 Web/APK 仍可能请求旧地址。
- `SERVER_HOST` 是 SSH 连接服务器用的 IP；`SERVER_ORIGIN` 是用户访问和前端请求接口用的域名，两者可以不同。

## 5. 配置服务器 SSH 密钥

### 5.1 在本机生成部署专用密钥

Windows PowerShell：

```powershell
ssh-keygen -t ed25519 -C "aituan-github-actions" -f "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519"
```

生成：

```text
%USERPROFILE%\.ssh\aituan_github_actions_ed25519
%USERPROFILE%\.ssh\aituan_github_actions_ed25519.pub
```

查看公钥：

```powershell
Get-Content "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519.pub"
```

### 5.2 把公钥放进服务器

如果当前用 root 登录：

```powershell
ssh root@8.220.192.106
```

服务器上执行：

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
```

把 `.pub` 文件里的整行公钥粘进去，保存后：

```bash
chmod 600 ~/.ssh/authorized_keys
```

### 5.3 本机测试密钥登录

```powershell
ssh -i "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519" root@8.220.192.106
```

如果 GitHub Secret `SERVER_USER` 填的不是 root，就把命令中的 root 换成对应用户名。

### 5.4 获取 known hosts

PowerShell：

```powershell
ssh-keyscan -H 8.220.192.106
```

如果没有 `ssh-keyscan`：

```powershell
& "C:\Program Files\Git\usr\bin\ssh-keyscan.exe" -H 8.220.192.106
```

把输出完整复制到 GitHub Secret：

```text
SERVER_KNOWN_HOSTS
```

### 5.5 配置私钥 Secret

查看私钥完整内容：

```powershell
Get-Content "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519" -Raw
```

复制到 GitHub Secret：

```text
SERVER_SSH_KEY
```

必须包含：

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

不要发给别人，不要写入文档。

## 6. 服务器目录和 `.env` 准备

下面命令在服务器执行。

### 6.1 进入部署目录

```bash
cd /opt/aituan/app
```

确认真实配置存在：

```bash
test -f deploy/.env && echo "deploy/.env exists"
test -f .config && echo ".config exists"
```

如果不存在，不要继续，先按服务器部署文档补齐。

### 6.2 备份 `.env`

```bash
mkdir -p /opt/aituan/backups
cp deploy/.env "/opt/aituan/backups/deploy.env.before-cicd-$(date +%Y%m%d-%H%M%S)"
```

### 6.3 添加 CI/CD 变量

在服务器执行：

```bash
grep -q '^AITUAN_IMAGE_REGISTRY=' deploy/.env || printf '\nAITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan\n' >> deploy/.env
grep -q '^AITUAN_IMAGE_TAG=' deploy/.env || printf 'AITUAN_IMAGE_TAG=sha-example\n' >> deploy/.env
grep -q '^AITUAN_DOWNLOADS_DIR=' deploy/.env || printf 'AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads\n' >> deploy/.env
```

说明：

| 变量 | 值 | 说明 |
| --- | --- | --- |
| `AITUAN_IMAGE_REGISTRY` | `ghcr.io/puaa-team/aituan` | workflow 推送镜像的前缀。 |
| `AITUAN_IMAGE_TAG` | `sha-example` | 首次占位；真正部署时 Actions 会改成真实 `sha-xxxxxxx`。 |
| `AITUAN_DOWNLOADS_DIR` | `/opt/aituan/data/downloads` | APK 下载目录挂载来源。 |

只检查这三项，不要打印完整 `.env`：

```bash
grep '^AITUAN_IMAGE_' deploy/.env
grep '^AITUAN_DOWNLOADS_DIR=' deploy/.env
```

### 6.4 准备目录

```bash
mkdir -p /opt/aituan/data/downloads
mkdir -p /opt/aituan/backups
```

如果已有本地打包出的服务器版 APK，可以上传到：

```text
/opt/aituan/data/downloads/aituan-user-server-debug.apk
```

后续访问：

```text
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

## 7. GHCR 镜像权限和服务器登录

### 7.1 先尝试公开包或使用默认权限

第一次运行 `aituan-deploy` 后，GitHub 会生成 GHCR package：

```text
ghcr.io/puaa-team/aituan/backend:<tag>
ghcr.io/puaa-team/aituan/web:<tag>
```

如果 package 设为 public，服务器不需要 `docker login ghcr.io`。

### 7.2 如果 package 是 private

需要在服务器登录 GHCR。

准备一个只读 GitHub Token，然后服务器执行：

```bash
echo "<只读 GHCR token>" | docker login ghcr.io -u <GitHub 用户名或机器人用户名> --password-stdin
```

成功后显示：

```text
Login Succeeded
```

注意：

- Token 不要写入 `deploy/.env`。
- Token 不要写入仓库和文档。
- 尽量用只读权限。

## 8. 首次运行 CI/CD 部署

### 8.1 先运行 CI

GitHub：

```text
Actions -> aituan-ci -> Run workflow
```

确认：

- Backend tests 通过。
- Web build 通过。
- Flutter analyze/test/web build 通过。

### 8.2 手动运行部署 workflow

GitHub：

```text
Actions -> aituan-deploy -> Run workflow
```

选择：

```text
deploy = true
```

如果你配置了 `production` required reviewers，GitHub 会要求审批。

workflow 会做：

1. 后端测试和打包；
2. 商家端、后台端、用户端 Web 构建；
3. 构建 backend/web 镜像；
4. 推送 GHCR；
5. 上传 `deploy/docker-compose.cicd.yml` 到服务器；
6. 修改服务器 `deploy/.env` 中的 `AITUAN_IMAGE_REGISTRY` 和 `AITUAN_IMAGE_TAG`；
7. 执行：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
curl -fsS http://127.0.0.1/actuator/health
```

## 9. 部署后验证

本机或浏览器访问：

```text
https://aituan.2b.gs/actuator/health
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
```

预期：

- 健康检查返回 `200` 和 `{"status":"UP"}`。
- 各 Web 页面能打开。
- 浏览器 Network 中接口请求应为 `https://aituan.2b.gs/api/...`。
- 不应出现旧 IP 或 `localhost:8080`。

服务器查看状态：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml ps
```

查看日志：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs -f backend
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs -f nginx
```

## 10. Android APK 自动打包与上传

### 10.1 手动触发 APK workflow

GitHub：

```text
Actions -> aituan-android-apk -> Run workflow
```

输入：

| 输入项 | 建议 |
| --- | --- |
| `api_origin` | 留空，使用 `SERVER_ORIGIN=https://aituan.2b.gs` |
| `upload_to_server` | 需要发布下载时选 `true` |
| `apk_name` | `aituan-user-server-debug.apk` |

workflow 会：

1. 读取 `SERVER_ORIGIN`；
2. 构建 debug APK；
3. 上传 GitHub Actions Artifact；
4. 如 `upload_to_server=true`，上传到服务器 `AITUAN_DOWNLOADS_DIR`。

### 10.2 APK 下载验证

如果上传服务器成功，访问：

```text
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

### 10.3 版本号要求

如果这是要给用户安装的新 APK，运行 workflow 前先递增：

```text
apps/user_app/pubspec.yaml
```

例如：

```yaml
version: 1.1.4+21
```

## 11. HTTPS 与证书配置

当前教程使用：

```text
https://aituan.2b.gs
```

服务器侧需要已经完成：

1. DNS A 记录指向 `8.220.192.106`。
2. 云安全组和本机防火墙开放 80/443。
3. 使用独立的 `deploy/docker-compose.acme.yml` 暂时启动 HTTP/ACME 配置。
4. 用 Let's Encrypt 申请 `aituan.2b.gs` 证书。
5. 使用正式 `deploy/docker-compose.server.yml` 或 `deploy/docker-compose.cicd.yml` 启动 HTTPS Nginx。
6. GitHub Variable 保持：

```text
SERVER_ORIGIN=https://aituan.2b.gs
```

然后重新运行：

```text
Actions -> aituan-deploy -> Run workflow
Actions -> aituan-android-apk -> Run workflow
```

否则 Web / APK 仍可能请求旧 HTTP/IP 地址。完整证书申请与续期步骤见 `docs/stage-new-1/域名HTTPS证书部署说明.md`。

## 12. 回滚方案

### 12.1 回滚到上一版 CI/CD 镜像

在服务器 `deploy/.env` 中把：

```dotenv
AITUAN_IMAGE_TAG=sha-xxxxxxx
```

改成上一个稳定 tag，然后执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
curl -fsS http://127.0.0.1/actuator/health
```

### 12.2 切回旧手动部署

旧 Compose 没有被改，可以执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

说明：

- 这会回到旧的“服务器本地 build backend + Nginx 挂载 `deploy/artifacts/*`”方式。
- 前提是服务器上旧的 `deploy/artifacts/*` 仍然存在且是可用版本。

## 13. 常见问题

### 13.1 `Read and write permissions` 灰色怎么办？

先运行一次 `aituan-deploy`。如果 GHCR 推送成功，可以暂时不处理。

如果失败，找 `PUAA-Team` 组织 Owner 调整：

```text
PUAA-Team -> Settings -> Actions -> General -> Workflow permissions
```

或者改用 PAT 推 GHCR，但不优先推荐。

### 13.2 `docker compose pull` 提示 unauthorized 怎么办？

说明服务器没有 GHCR 读取权限。处理方式：

- 把 GHCR package 设为 public；或
- 在服务器执行 `docker login ghcr.io`。

### 13.3 页面打开了但接口还请求旧 IP 怎么办？

检查 GitHub Variable：

```text
SERVER_ORIGIN=https://aituan.2b.gs
```

然后重新运行 `aituan-deploy`。前端地址是在构建时写入的，改变量后必须重新构建镜像。

### 13.4 APK 还请求旧 IP 怎么办？

重新运行：

```text
Actions -> aituan-android-apk -> Run workflow
```

`api_origin` 留空，让它读取当前 `SERVER_ORIGIN`；或者手动填当前 Origin。

### 13.5 `502 Bad Gateway` 怎么办？

服务器查看后端日志：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml logs -f backend
```

重点查：

- MySQL 是否健康；
- `.config` 是否挂载成功；
- Flyway 迁移是否失败；
- 后端是否 OOM。

## 14. 配置清单

GitHub Secrets：

```text
SERVER_HOST=8.220.192.106
SERVER_PORT=22
SERVER_USER=<服务器部署用户名>
SERVER_SSH_KEY=<部署私钥完整内容>
SERVER_KNOWN_HOSTS=<ssh-keyscan -H 8.220.192.106 输出>
```

GitHub Variables：

```text
SERVER_ORIGIN=https://aituan.2b.gs
SERVER_APP_DIR=/opt/aituan/app
AUTO_DEPLOY_PRODUCTION=false 或不设置
```

服务器 `deploy/.env` 增量变量：

```dotenv
AITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan
AITUAN_IMAGE_TAG=sha-example
AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads
AITUAN_NGINX_SERVER_NAME=aituan.2b.gs
AITUAN_LETSENCRYPT_DIR=/etc/letsencrypt
AITUAN_CERTBOT_WEBROOT=/var/www/certbot
```

服务器目录：

```text
/opt/aituan/app
/opt/aituan/data/mysql
/opt/aituan/data/uploads
/opt/aituan/data/downloads
/opt/aituan/backups
```
