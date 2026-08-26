# GitHub Actions 权限、SSH 密钥与服务器首次 CI/CD 准备说明

> 本文回答三个问题：GitHub 上 `Read and write permissions` 为什么灰色不能选；服务器 SSH 能不能用密码登录，为什么 CI/CD 推荐密钥；首次使用 CI/CD 前服务器 `/opt/aituan/app/deploy/.env`、目录和 GHCR 登录应该怎么准备。

## 1. GitHub 上 `Read and write permissions` 为什么是灰色不能选？

### 1.1 结论

当前仓库远程地址是：

```text
https://github.com/PUAA-Team/aituan.git
```

这说明仓库属于 `PUAA-Team` 组织。GitHub 仓库页面里：

```text
Settings -> Actions -> General -> Workflow permissions
```

如果 `Read and write permissions` 是灰色不能选，最常见原因是：

- 组织级别 GitHub Actions 策略限制了仓库默认 `GITHUB_TOKEN` 权限；
- 或企业级策略比组织/仓库更严格；
- 仓库管理员没有权限覆盖组织/企业的更严格默认值。

GitHub 官方文档说明：如果组织或企业选择了更严格的默认权限，仓库层面的更宽松选项会被禁用。也就是说，这通常不是本地代码问题，而是 GitHub 组织/企业设置问题。

### 1.2 这会不会影响我们现在的 workflow？

本项目新增的 `.github/workflows/deploy.yml` 已经在 workflow 文件中声明：

```yaml
permissions:
  contents: read
  packages: write
```

它的目的就是让 GitHub Actions 使用 `GITHUB_TOKEN` 推送 GHCR 镜像。

但如果组织级策略把 `GITHUB_TOKEN` 的最大权限限制得更严格，workflow 运行到推送 GHCR 镜像时仍可能失败，例如出现：

```text
permission_denied
unauthorized
write_package denied
```

因此建议先运行一次 workflow 验证。如果推镜像失败，再处理组织权限。

### 1.3 怎么解决？

#### 方案 A：让组织 Owner 调整 Actions 默认权限（推荐）

需要 `PUAA-Team` 组织 Owner 登录 GitHub：

```text
GitHub -> PUAA-Team -> Settings -> Actions -> General -> Workflow permissions
```

将默认 workflow 权限调整为允许写入，或允许仓库自行设置更宽松权限。

然后回到仓库：

```text
PUAA-Team/aituan -> Settings -> Actions -> General -> Workflow permissions
```

确认 `Read and write permissions` 可以选择。

#### 方案 B：保持仓库页面灰色，先依赖 workflow 内的 `permissions` 字段

如果组织只是把默认值设为 read-only，但没有禁止 workflow 按 job 提升权限，那么 `.github/workflows/deploy.yml` 里的：

```yaml
permissions:
  contents: read
  packages: write
```

可能已经足够。实际以首次 Actions 运行结果为准。

#### 方案 C：使用 PAT 推送 GHCR（备选，不优先）

如果 `GITHUB_TOKEN` 无法获得 `packages: write`，可以在 GitHub Secrets 中配置一个专用 Token，然后 workflow 用这个 Token 登录 GHCR。

不推荐优先使用 PAT 的原因：

- PAT 生命周期和权限需要单独维护；
- 泄露风险比自动生成的 `GITHUB_TOKEN` 更高；
- GitHub 官方对 Actions 发布同仓库关联 package 更推荐用 `GITHUB_TOKEN`。

如果必须用 PAT，建议只给最小权限，例如只用于 package 写入，并放在 GitHub Environment Secret 中，不写入仓库。

## 2. 服务器 SSH 能不能通过密码登录？

### 2.1 技术上可以，但 CI/CD 不推荐

服务器 SSH 是否能用密码登录取决于服务器 `/etc/ssh/sshd_config` 中类似配置：

```text
PasswordAuthentication yes
```

如果服务器开启了密码登录，那么人在终端里可以：

```bash
ssh user@8.220.192.106
```

然后输入密码。

但是 GitHub Actions 里不推荐用密码登录，原因是：

- GitHub-hosted runner 默认不适合交互式输入密码；
- 需要额外安装 `sshpass` 或第三方 action，安全性更差；
- 密码一旦放进 GitHub Secrets，泄露后的影响范围通常比部署专用密钥更大；
- CI/CD 无法方便地限制“这把凭据只用于部署”。

本项目 `.github/workflows/deploy.yml` 已按 SSH 私钥方式实现，不支持密码登录。

### 2.2 推荐方式：部署专用 SSH 密钥

推荐生成一对专用密钥：

- 公钥：放到服务器部署用户的 `~/.ssh/authorized_keys`；
- 私钥：放到 GitHub `production` Environment Secret：`SERVER_SSH_KEY`；
- 服务器 host key：放到 GitHub Secret：`SERVER_KNOWN_HOSTS`。

这样 GitHub Actions 可以非交互式连接服务器，同时不用保存服务器密码。

## 3. 如何配置 SSH 密钥？

下面以服务器地址 `8.220.192.106` 为例。

### 3.1 在本机生成部署专用密钥

在 Windows PowerShell 执行：

```powershell
ssh-keygen -t ed25519 -C "aituan-github-actions" -f "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519"
```

执行时会询问 passphrase。为了 GitHub Actions 自动部署方便，可以直接回车留空；如果设置 passphrase，需要额外在 workflow 里处理，不建议本阶段增加复杂度。

生成后会得到：

```text
C:\Users\你的用户名\.ssh\aituan_github_actions_ed25519      # 私钥，放 GitHub Secret
C:\Users\你的用户名\.ssh\aituan_github_actions_ed25519.pub  # 公钥，放服务器 authorized_keys
```

查看公钥：

```powershell
Get-Content "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519.pub"
```

### 3.2 把公钥放到服务器

如果你当前还能用密码 SSH 登录服务器，可以先手动登录：

```bash
ssh root@8.220.192.106
```

在服务器上执行：

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
```

把本机 `.pub` 文件里的整行公钥粘进去，保存后执行：

```bash
chmod 600 ~/.ssh/authorized_keys
```

如果服务器上没有 `nano`，可以用 `vi`，或者让运维/云控制台把公钥写入该用户的 `authorized_keys`。

### 3.3 本机测试密钥登录

回到本机 PowerShell：

```powershell
ssh -i "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519" <部署用户>@8.220.192.106
```

如果无需输入服务器密码即可登录，说明密钥配置成功。

### 3.4 获取 `SERVER_KNOWN_HOSTS`

在本机执行：

```powershell
ssh-keyscan -H 8.220.192.106
```

把输出的全部内容复制到 GitHub Secret：

```text
SERVER_KNOWN_HOSTS
```

如果本机没有 `ssh-keyscan`，可用 Git 自带 OpenSSH：

```powershell
& "C:\Program Files\Git\usr\bin\ssh-keyscan.exe" -H 8.220.192.106
```

### 3.5 配置 GitHub Secrets

进入 GitHub：

```text
PUAA-Team/aituan -> Settings -> Environments -> production -> Environment secrets
```

添加：

| Secret | 内容 |
| --- | --- |
| `SERVER_HOST` | `8.220.192.106` |
| `SERVER_USER` | 服务器部署用户名 |
| `SERVER_PORT` | 通常是 `22` |
| `SERVER_SSH_KEY` | 私钥文件 `aituan_github_actions_ed25519` 的完整内容 |
| `SERVER_KNOWN_HOSTS` | `ssh-keyscan -H 8.220.192.106` 的输出 |

查看私钥内容时，在 PowerShell 执行：

```powershell
Get-Content "$env:USERPROFILE\.ssh\aituan_github_actions_ed25519" -Raw
```

复制时必须包含：

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

不要把私钥写进仓库、文档或聊天记录。

## 4. 首次部署前服务器需要怎么准备？

下面命令需要在服务器上执行，不是 GitHub 网页端配置。

> 注意：以下操作会修改服务器 `/opt/aituan/app/deploy/.env` 并创建目录。执行前建议确认当前服务器部署目录就是 `/opt/aituan/app`。

### 4.1 进入服务器项目目录

```bash
cd /opt/aituan/app
```

确认关键文件存在：

```bash
test -f deploy/.env && echo "deploy/.env exists"
test -f .config && echo ".config exists"
```

如果任一文件不存在，不要继续执行，先按服务器部署文档补齐真实配置。

### 4.2 备份服务器真实 `.env`

```bash
mkdir -p /opt/aituan/backups
cp deploy/.env "/opt/aituan/backups/deploy.env.before-cicd-$(date +%Y%m%d-%H%M%S)"
```

### 4.3 在 `deploy/.env` 中增加 CI/CD 镜像变量

当前仓库是 `PUAA-Team/aituan`，workflow 会转成小写镜像路径，因此 registry 建议为：

```text
ghcr.io/puaa-team/aituan
```

在服务器执行：

```bash
grep -q '^AITUAN_IMAGE_REGISTRY=' deploy/.env || printf '\nAITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan\n' >> deploy/.env
grep -q '^AITUAN_IMAGE_TAG=' deploy/.env || printf 'AITUAN_IMAGE_TAG=sha-example\n' >> deploy/.env
grep -q '^AITUAN_DOWNLOADS_DIR=' deploy/.env || printf 'AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads\n' >> deploy/.env
```

说明：

- `AITUAN_IMAGE_REGISTRY=ghcr.io/puaa-team/aituan`：镜像仓库前缀。
- `AITUAN_IMAGE_TAG=sha-example`：占位 tag；第一次真正部署时，GitHub Actions 会自动改成真实 `sha-xxxxxxx`。
- `AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads`：Nginx 的 `/downloads/` 挂载目录，用于保留 APK 下载文件。

查看是否写入成功：

```bash
grep '^AITUAN_IMAGE_' deploy/.env
grep '^AITUAN_DOWNLOADS_DIR=' deploy/.env
```

不要执行 `cat deploy/.env` 后把完整输出发到公开位置，因为里面有数据库密码。

### 4.4 准备目录

```bash
mkdir -p /opt/aituan/data/downloads
mkdir -p /opt/aituan/backups
```

如果已有服务器版 APK，后续可手动放到：

```text
/opt/aituan/data/downloads/aituan-user-server-debug.apk
```

这样 Nginx 仍可通过：

```text
https://aituan.2b.gs/downloads/aituan-user-server-debug.apk
```

提供下载。CI/CD 默认不重新构建 APK。

### 4.5 如果 GHCR 镜像是私有的，服务器执行 `docker login ghcr.io`

如果 GitHub Packages 中 backend/web 镜像设为 public，可以跳过本节。

如果镜像是 private，需要准备一个只读 GitHub token。建议权限最小化，只用于读取 package。

服务器执行：

```bash
echo "<你的只读 GHCR token>" | docker login ghcr.io -u <你的 GitHub 用户名或机器人用户名> --password-stdin
```

成功后会看到类似：

```text
Login Succeeded
```

不要把 token 写入 `deploy/.env`，也不要写入文档。

### 4.6 验证 CI/CD Compose 配置

首次 GitHub Actions 部署时会上传 `deploy/docker-compose.cicd.yml`。如果你已经把该文件同步到服务器，可以执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml config
```

能正常展开配置，说明 `.env` 变量基本齐全。

### 4.7 首次正式部署

GitHub 配置完成后，进入：

```text
GitHub -> PUAA-Team/aituan -> Actions -> aituan-deploy -> Run workflow
```

选择：

```text
deploy = true
```

运行后流程会：

1. 跑测试；
2. 构建后端和前端产物；
3. 构建并推送 GHCR 镜像；
4. 上传 `deploy/docker-compose.cicd.yml` 到服务器；
5. 修改服务器 `deploy/.env` 中的 `AITUAN_IMAGE_REGISTRY` 和 `AITUAN_IMAGE_TAG`；
6. 执行 `docker compose pull && docker compose up -d --remove-orphans`；
7. 检查 `http://127.0.0.1/actuator/health`。

## 5. 常见问题

### 5.1 `Read and write permissions` 灰色，但 workflow 里写了 `packages: write`，还需要管吗？

先运行一次 `aituan-deploy`。如果 GHCR push 成功，就暂时不用改组织权限。

如果 push 失败，再找 `PUAA-Team` 组织 Owner 调整 Actions 权限，或者改用专用 PAT。

### 5.2 可以先用 root 用户部署吗？

技术上可以，但不推荐长期使用。建议后续创建专用部署用户，并只授予它访问 `/opt/aituan/app` 和执行 Docker 的必要权限。

### 5.3 如果服务器禁用了密码登录怎么办？

这是正常的安全配置。用云控制台、已有 root 会话或已有密钥登录一次，把部署公钥写入目标用户的 `~/.ssh/authorized_keys` 即可。

### 5.4 如果服务器禁用了公钥登录怎么办？

需要有服务器管理员权限，检查 `/etc/ssh/sshd_config`：

```text
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
```

修改 SSH 配置属于高风险系统配置操作，执行前必须确认当前仍有可用登录方式，避免把自己锁在服务器外。

### 5.5 如何回滚？

把服务器 `deploy/.env` 中的：

```dotenv
AITUAN_IMAGE_TAG=sha-xxxxxxx
```

改回上一个稳定 tag，然后执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
curl -fsS http://127.0.0.1/actuator/health
```

如果要切回旧手动部署链路：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

## 6. 官方资料参考

- GitHub 组织级 Actions 权限限制说明：<https://docs.github.com/en/organizations/managing-organization-settings/disabling-or-limiting-github-actions-for-your-organization>
- GitHub 仓库级 Actions 设置说明：<https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository>
- GitHub workflow `permissions` 语法：<https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- GitHub Container Registry 说明：<https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- GitHub Actions 发布 package 官方说明：<https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/about-packaging-with-github-actions>
- GitHub Actions Secrets 官方说明：<https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets>
- GitHub SSH deploy keys 官方说明：<https://docs.github.com/authentication/connecting-to-github-with-ssh/managing-deploy-keys>
