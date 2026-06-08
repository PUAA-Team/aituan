# Stage7 流水线部署与 CI/CD 可行性方案

> 目标：探索并设计一套适合当前爱团项目的流水线部署方案，支持 GitHub Actions 自动测试、自动打包、构建并推送 Docker 镜像、服务器 Docker Compose 自动拉取更新镜像并完成部署。本文已从“可行性方案”补充为“落地说明”，现有手动部署脚本与 `deploy/docker-compose.server.yml` 保持不变。

## 1. 结论先行

当前项目具备落地 CI/CD 的基础条件，推荐分两阶段演进：

1. **短期稳妥方案：Actions 自动测试 + 打包产物 + SSH 上传 + 服务器 `docker compose up -d --build`**
   - 改动少，沿用现有 `deploy/docker-compose.server.yml`。
   - 仍然是服务器本地构建后端运行镜像，Web 静态产物通过目录挂载。
   - 不完全满足“Compose 自动拉取更新镜像”。

2. **推荐正式方案：Actions 自动测试 + 构建后端/前端镜像 + 推送 GHCR + 服务器 `docker compose pull && up -d`**
   - 后端、Nginx 静态站点都镜像化。
   - 服务器只负责拉镜像、保留数据卷、重启容器，不再承担 Maven/npm/Flutter 构建。
   - 最符合 CI/CD、可回滚、可审计，也符合 2C2G 服务器资源约束。

综合课程演示、服务器资源和现有部署方式，建议 Stage7 后续按 **推荐正式方案** 落地。

### 1.1 本次已落地文件

本次落地采用“新增并行 CI/CD 链路，不改现有手动部署链路”的方式。新增或更新文件如下：

| 文件 | 作用 | 是否影响现有手动部署 |
| --- | --- | --- |
| `.github/workflows/ci.yml` | PR / 手动触发的后端、Web、Flutter 校验，不部署服务器。 | 否 |
| `.github/workflows/deploy.yml` | main / 手动触发的测试、构建、推送 GHCR 镜像和可控 SSH 部署。 | 否 |
| `.github/workflows/android-apk.yml` | 手动触发的用户端 Android debug APK 打包，可选上传服务器下载目录。 | 否 |
| `deploy/docker-compose.cicd.yml` | CI/CD 专用 Compose，服务器拉取远程 backend/web 镜像。 | 否，旧 `deploy/docker-compose.server.yml` 保留 |
| `deploy/web/Dockerfile` | 构建包含 landing、用户端 Web、商家端 Web、后台端 Web 的 Nginx 静态站点镜像。 | 否 |
| `deploy/web/Dockerfile.dockerignore` | Web 镜像专用 build context 白名单，避免调整现有 `deploy/.dockerignore`。 | 否 |
| `deploy/.env.example` | 增加 CI/CD 镜像变量模板，不包含真实密钥。 | 否 |

保留不动的现有链路：

- `deploy/docker-compose.server.yml`
- `scripts/build/*.ps1`
- 服务器真实 `/opt/aituan/app/deploy/.env`
- 服务器真实 `/opt/aituan/app/.config`

## 2. 当前项目部署现状

当前部署文档见：`docs/服务器DockerCompose部署完整说明.md`。

现有链路：

```text
本机 Windows 构建后端 JAR / Web 静态产物 / APK
  ↓
产物放入 deploy/artifacts/*
  ↓
上传到 /opt/aituan/app
  ↓
服务器 docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

当前 Compose 特点：

- `mysql` 使用远程基础镜像 `mysql:8.0`。
- `nginx` 使用远程基础镜像 `nginx:1.27-alpine`，但业务静态产物通过宿主机目录挂载。
- `backend` 使用 `deploy/backend/Dockerfile` 在服务器本地构建 `aituan-backend:server`，JAR 来自 `deploy/artifacts/backend/aituan-backend.jar`。
- 数据库与上传文件已经通过宿主机目录持久化，适合无状态服务镜像化。

因此当前方案可以做自动化上传部署，但要实现“Docker Compose 自动拉取更新镜像”，需要把业务后端和 Web 产物都变成可拉取的远程镜像。

## 3. 推荐目标架构

```text
GitHub main 分支 / 手动 workflow_dispatch
  ↓
GitHub Actions
  ├─ 后端：Java 17 + Maven test/package
  ├─ 商家端/后台端：Node 24 + npm ci + npm run build
  ├─ 用户端：Flutter analyze/test + web build + 可选 Android APK build
  ├─ Docker Buildx 构建镜像
  ├─ 推送镜像到 GHCR：ghcr.io/<owner>/<repo>/backend:<tag>
  └─ 推送镜像到 GHCR：ghcr.io/<owner>/<repo>/web:<tag>
  ↓
Actions 通过 SSH 通知服务器
  ↓
服务器 /opt/aituan/app
  ├─ docker compose pull
  ├─ docker compose up -d
  └─ curl /actuator/health 验证
```

推荐镜像拆分：

| 镜像 | 内容 | 是否无状态 | 说明 |
| --- | --- | --- | --- |
| `backend` | Spring Boot JAR | 是 | 运行时挂载 `.config` 和 uploads。 |
| `web` | Nginx 配置 + landing + user-web + merchant-web + admin-web + downloads | 是 | 静态产物随镜像发布；也可后续把 APK 移到 Release 或对象存储。 |
| `mysql` | `mysql:8.0` | 否 | 继续使用数据卷，不进入业务镜像发布链路。 |

## 4. 镜像标签策略

不要只依赖 `latest`，否则回滚和排查困难。推荐同时推送：

| 标签 | 用途 |
| --- | --- |
| `sha-<短提交号>` | 每次构建唯一、推荐部署和回滚使用。 |
| `main` | main 分支最新镜像，便于手动查看。 |
| `vX.Y.Z` | 打正式版本时使用。 |

服务器 Compose 推荐通过 `.env` 固定当前部署标签：

```dotenv
AITUAN_IMAGE_REGISTRY=ghcr.io/<owner>/<repo>
AITUAN_IMAGE_TAG=sha-abcdef1
```

Compose 中引用：

```yaml
backend:
  image: ${AITUAN_IMAGE_REGISTRY}/backend:${AITUAN_IMAGE_TAG}
  pull_policy: always

nginx:
  image: ${AITUAN_IMAGE_REGISTRY}/web:${AITUAN_IMAGE_TAG}
  pull_policy: always
```

这样回滚只需把 `AITUAN_IMAGE_TAG` 改回旧值，再执行 `docker compose pull && docker compose up -d`。

## 5. 需要新增或调整的项目文件

> 本节原为落地清单；当前已新增 CI/CD 专用文件，但现有手动部署文件仍保持不变。

### 5.1 新增 CI/CD 专用 Compose 文件

已新增：`deploy/docker-compose.cicd.yml`。

核心差异：

- `backend` 不再 `build`，改为远程 `image`。
- `nginx` 不再挂载 `deploy/artifacts/*`，改为远程 `web` 镜像内置静态产物。
- `mysql`、数据卷、`.config`、uploads 挂载继续保留。

示例：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: aituan-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE:-aituan_dev}
      MYSQL_USER: ${MYSQL_USER:?set MYSQL_USER in deploy/.env}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:?set MYSQL_PASSWORD in deploy/.env}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?set MYSQL_ROOT_PASSWORD in deploy/.env}
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+08:00
      - --innodb-buffer-pool-size=256M
      - --max-connections=50
      - --performance-schema=OFF
      - --table-open-cache=256
      - --tmp-table-size=32M
      - --max-heap-table-size=32M
    volumes:
      - ${AITUAN_DATA_DIR:-/opt/aituan/data}/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -u\"$${MYSQL_USER}\" -p\"$${MYSQL_PASSWORD}\" --silent"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 30s
    networks:
      - aituan

  backend:
    image: ${AITUAN_IMAGE_REGISTRY:?set AITUAN_IMAGE_REGISTRY}/backend:${AITUAN_IMAGE_TAG:?set AITUAN_IMAGE_TAG}
    pull_policy: always
    container_name: aituan-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8080
      AITUAN_DATASOURCE_URL: "jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-aituan_dev}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
      AITUAN_DATASOURCE_USERNAME: "${MYSQL_USER:?set MYSQL_USER in deploy/.env}"
      AITUAN_DATASOURCE_PASSWORD: "${MYSQL_PASSWORD:?set MYSQL_PASSWORD in deploy/.env}"
      AITUAN_CONFIG_FILE: /app/.config
      AITUAN_UPLOAD_ROOT: /data/uploads
      AITUAN_UPLOAD_PUBLIC_PREFIX: /api/common/files
      JAVA_TOOL_OPTIONS: "-Xms128m -Xmx512m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
      TZ: Asia/Shanghai
    volumes:
      - ${AITUAN_DATA_DIR:-/opt/aituan/data}/uploads:/data/uploads
      - ${AITUAN_CONFIG_HOST_FILE:-../.config}:/app/.config:ro
    expose:
      - "8080"
    networks:
      - aituan

  nginx:
    image: ${AITUAN_IMAGE_REGISTRY:?set AITUAN_IMAGE_REGISTRY}/web:${AITUAN_IMAGE_TAG:?set AITUAN_IMAGE_TAG}
    pull_policy: always
    container_name: aituan-nginx
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - aituan

networks:
  aituan:
    name: aituan-network
```

### 5.2 新增 Web 镜像 Dockerfile

已新增：

- `deploy/web/Dockerfile`
- `deploy/web/Dockerfile.dockerignore`

```dockerfile
FROM nginx:1.27-alpine

COPY deploy/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
COPY deploy/artifacts/landing /usr/share/nginx/html/landing
COPY deploy/artifacts/user-web /usr/share/nginx/html/web
COPY deploy/artifacts/merchant-web /usr/share/nginx/html/merchant
COPY deploy/artifacts/admin-web /usr/share/nginx/html/admin

RUN mkdir -p /usr/share/nginx/html/downloads
```

当前 `deploy/.dockerignore` 排除了 `artifacts/merchant-web/`、`artifacts/admin-web/`、`artifacts/downloads/`。本次没有改它，而是让 Web 镜像在 workflow 中使用仓库根目录作为 context，并通过 `deploy/web/Dockerfile.dockerignore` 白名单只放行 Web 镜像需要的 `deploy/nginx` 与 `deploy/artifacts/*`。APK 下载目录由 CI/CD Compose 挂载 `${AITUAN_DOWNLOADS_DIR:-/opt/aituan/data/downloads}`，不默认打入 Web 镜像。

### 5.3 后端镜像 Dockerfile

可以复用当前 `deploy/backend/Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S aituan && adduser -S aituan -G aituan
COPY --chown=aituan:aituan artifacts/backend/aituan-backend.jar /app/aituan-backend.jar
USER aituan
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/aituan-backend.jar"]
```

CI 中先把 Maven 构建出的 JAR 复制到 `deploy/artifacts/backend/aituan-backend.jar`，再执行 Docker build。

## 6. GitHub Secrets / Variables 设计

建议使用 GitHub Environments（如 `production`）保存生产部署变量，配合审批保护。

| 名称 | 类型 | 说明 |
| --- | --- | --- |
| `SERVER_HOST` | Secret | 服务器 IP 或域名，如 `182.92.238.178`。 |
| `SERVER_PORT` | Variable/Secret | SSH 端口，默认 `22`。 |
| `SERVER_USER` | Secret | SSH 登录用户，建议不是 root。 |
| `SERVER_SSH_KEY` | Secret | 只用于部署的私钥，建议设置最小权限。 |
| `SERVER_KNOWN_HOSTS` | Secret | 服务器 SSH host key，避免盲目信任。 |
| `SERVER_APP_DIR` | Variable | `/opt/aituan/app`。 |
| `SERVER_ORIGIN` | Variable | 当前对外访问 Origin，用于前端/API 构建；填写时必须带 `http://` 或 `https://`，不要包含 `/api`。 |
| `AUTO_DEPLOY_PRODUCTION` | Variable | 可选。设为 `true` 时 main push 在镜像推送后进入 production 部署；不设时 main push 只构建推镜像，需手动运行 `workflow_dispatch` 部署。 |
| `GHCR_READ_TOKEN` | Secret | 如果 GHCR 包是私有的，服务器 `docker login ghcr.io` 需要只读 token。 |

GHCR 推镜像通常可直接使用 `GITHUB_TOKEN`，但需要 workflow 权限：

```yaml
permissions:
  contents: read
  packages: write
```

## 7. 服务器一次性准备步骤

### 7.1 安装 Docker 和 Compose plugin

服务器需要可用的：

```bash
docker --version
docker compose version
```

### 7.2 准备目录和敏感配置

```bash
sudo mkdir -p /opt/aituan/app /opt/aituan/data/mysql /opt/aituan/data/uploads /opt/aituan/backups
cd /opt/aituan/app
```

放置以下文件：

```text
/opt/aituan/app/deploy/docker-compose.cicd.yml
/opt/aituan/app/deploy/.env
/opt/aituan/app/.config
```

`deploy/.env` 增加镜像变量：

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=aituan
MYSQL_PASSWORD=替换为强密码
MYSQL_ROOT_PASSWORD=替换为强密码
AITUAN_DATA_DIR=/opt/aituan/data
AITUAN_CONFIG_HOST_FILE=../.config
AITUAN_IMAGE_REGISTRY=ghcr.io/<owner>/<repo>
AITUAN_IMAGE_TAG=sha-abcdef1
AITUAN_DOWNLOADS_DIR=/opt/aituan/data/downloads
```

### 7.3 登录镜像仓库

如果 GHCR 包设为公开，可以跳过服务器登录。若包是私有，需要在服务器上一次性登录：

```bash
echo "<GHCR_READ_TOKEN>" | docker login ghcr.io -u <github-user-or-bot> --password-stdin
```

不要把 Token 写进仓库。可以只保存在服务器 Docker credential store 或 CI Secret 中。

### 7.4 验证 Compose 配置

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml config
```

## 8. GitHub Actions 工作流建议

### 8.1 PR 校验工作流

触发：`pull_request`。

目标：只跑测试和构建，不部署。

检查项：

- 后端：`mvn test`，验证 Flyway + H2/MySQL mode 空库迁移能力。
- 商家端：`npm ci && npm run build`。
- 后台端：`npm ci && npm run build`。
- 用户端：`flutter analyze && flutter test`。

### 8.2 main 分支发布工作流

触发：`push` 到 `main` 或手动 `workflow_dispatch`。

目标：测试通过后构建镜像、推送镜像、部署服务器。

示例骨架：

```yaml
name: aituan-ci-cd

on:
  push:
    branches: ["main"]
  workflow_dispatch:

permissions:
  contents: read
  packages: write

env:
  REGISTRY: ghcr.io
  IMAGE_NAMESPACE: ${{ github.repository }}
  SERVER_ORIGIN: ${{ vars.SERVER_ORIGIN }}

jobs:
  test-build-push:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.image-tag }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Compute image tag
        id: meta
        run: echo "image-tag=sha-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Backend test and package
        run: |
          mvn -f services/backend/pom.xml test
          mvn -f services/backend/pom.xml -DskipTests package
          mkdir -p deploy/artifacts/backend
          cp services/backend/target/aituan-backend-0.0.1-SNAPSHOT.jar deploy/artifacts/backend/aituan-backend.jar

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: "24"
          cache: npm
          cache-dependency-path: |
            apps/merchant_web/package-lock.json
            apps/admin_web/package-lock.json

      - name: Build merchant web
        env:
          VITE_API_BASE_URL: ${{ env.SERVER_ORIGIN }}
        run: |
          npm ci --prefix apps/merchant_web
          npm run build --prefix apps/merchant_web -- --base=/merchant/ --outDir "$PWD/deploy/artifacts/merchant-web" --emptyOutDir

      - name: Build admin web
        env:
          VITE_API_BASE_URL: ${{ env.SERVER_ORIGIN }}
        run: |
          npm ci --prefix apps/admin_web
          npm run build --prefix apps/admin_web -- --base=/admin/ --outDir "$PWD/deploy/artifacts/admin-web" --emptyOutDir

      - name: Set up Flutter
        uses: subosito/flutter-action@v2
        with:
          channel: stable
          flutter-version: "3.41.6"
          cache: true

      - name: Analyze and test Flutter user app
        working-directory: apps/user_app
        run: |
          flutter pub get
          flutter analyze
          flutter test

      - name: Build Flutter web
        working-directory: apps/user_app
        run: |
          flutter build web --base-href /web/ --dart-define=API_BASE_URL=${{ env.SERVER_ORIGIN }} --output "$GITHUB_WORKSPACE/deploy/artifacts/user-web"

      - name: Sync landing page
        run: |
          rm -rf deploy/artifacts/landing
          mkdir -p deploy/artifacts/landing
          cp -R deploy/landing/. deploy/artifacts/landing/

      # 可选：如要把 APK 也放进 web 镜像，可打开本步骤；镜像会明显变大。
      # - name: Build Android APK
      #   working-directory: apps/user_app
      #   run: |
      #     flutter build apk --debug --dart-define=API_BASE_URL=${{ env.SERVER_ORIGIN }} --dart-define=LOCATION_DEBUG_ERRORS=true
      #     mkdir -p "$GITHUB_WORKSPACE/deploy/artifacts/downloads"
      #     cp build/app/outputs/flutter-apk/app-debug.apk "$GITHUB_WORKSPACE/deploy/artifacts/downloads/aituan-user-server-debug.apk"

      - name: Ensure downloads dir exists
        run: mkdir -p deploy/artifacts/downloads

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push backend image
        uses: docker/build-push-action@v6
        with:
          context: deploy
          file: deploy/backend/Dockerfile
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAMESPACE }}/backend:${{ steps.meta.outputs.image-tag }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAMESPACE }}/backend:main

      - name: Build and push web image
        uses: docker/build-push-action@v6
        with:
          context: .
          file: deploy/web/Dockerfile
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAMESPACE }}/web:${{ steps.meta.outputs.image-tag }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAMESPACE }}/web:main

  deploy:
    runs-on: ubuntu-latest
    needs: test-build-push
    environment: production
    steps:
      - name: Prepare SSH
        run: |
          mkdir -p ~/.ssh
          chmod 700 ~/.ssh
          printf '%s\n' "${{ secrets.SERVER_SSH_KEY }}" > ~/.ssh/aituan_deploy_key
          chmod 600 ~/.ssh/aituan_deploy_key
          printf '%s\n' "${{ secrets.SERVER_KNOWN_HOSTS }}" > ~/.ssh/known_hosts

      - name: Deploy by Docker Compose pull
        env:
          IMAGE_TAG: ${{ needs.test-build-push.outputs.image-tag }}
          SERVER_HOST: ${{ secrets.SERVER_HOST }}
          SERVER_PORT: ${{ secrets.SERVER_PORT }}
          SERVER_USER: ${{ secrets.SERVER_USER }}
          SERVER_APP_DIR: ${{ vars.SERVER_APP_DIR }}
        run: |
          ssh -i ~/.ssh/aituan_deploy_key -p "${SERVER_PORT:-22}" "${SERVER_USER}@${SERVER_HOST}" \
            "set -e; cd '${SERVER_APP_DIR:-/opt/aituan/app}'; \
             cp deploy/.env deploy/.env.before-ci; \
             if grep -q '^AITUAN_IMAGE_TAG=' deploy/.env; then sed -i 's/^AITUAN_IMAGE_TAG=.*/AITUAN_IMAGE_TAG=${IMAGE_TAG}/' deploy/.env; else printf '\nAITUAN_IMAGE_TAG=${IMAGE_TAG}\n' >> deploy/.env; fi; \
             docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull; \
             docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans; \
             curl -fsS http://127.0.0.1/actuator/health"
```

注意：上面 YAML 是方案骨架；本次实际落地已拆分为 `.github/workflows/ci.yml` 和 `.github/workflows/deploy.yml`。Web 镜像使用 `context: .` 与 `deploy/web/Dockerfile.dockerignore`，避免被现有 `deploy/.dockerignore` 误排除静态产物。

## 9. 自动测试、打包、部署的执行顺序

推荐顺序：

1. **静态和单元测试**
   - 后端：`mvn test`。
   - Flutter：`flutter analyze && flutter test`。
   - Web：`npm run build` 内置 `vue-tsc --noEmit`，可覆盖类型检查。

2. **产物构建**
   - 后端 JAR。
   - 商家端/后台端 Vite 静态产物。
   - 用户端 Flutter Web。
   - 用户端 Android APK（可选，耗时较长且显著增大 web 镜像）。

3. **镜像构建与推送**
   - `backend:<sha>`。
   - `web:<sha>`。
   - 同时可推 `main` 标签。

4. **服务器部署**
   - 更新服务器 `.env` 中的 `AITUAN_IMAGE_TAG`。
   - `docker compose pull` 拉取新镜像。
   - `docker compose up -d --remove-orphans` 重建变化容器。
   - `curl http://127.0.0.1/actuator/health` 验证后端健康。

5. **失败处理**
   - 任意测试失败：停止，不构建镜像，不部署。
   - 镜像推送失败：停止，不部署。
   - 部署健康检查失败：保留日志，按上一镜像标签回滚。

## 10. 服务器自动拉取更新镜像的方式

### 10.1 CI 主动触发部署（推荐）

GitHub Actions 在镜像推送成功后 SSH 到服务器执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
```

优点：

- 每次部署有 GitHub Actions 日志。
- 可接 GitHub Environment 审批。
- 可在失败时停止并保留现场。

### 10.2 服务器定时拉取（不推荐作为主方案）

可用 cron 每隔几分钟执行：

```bash
cd /opt/aituan/app && docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --pull always
```

问题：

- 无法精准绑定某次 commit。
- 部署失败反馈不集中。
- 如果使用 `latest/main` 标签，回滚困难。

除非只是课程演示环境，否则不建议用定时拉取替代 CI 主动部署。

### 10.3 Watchtower 类工具（暂不建议）

Watchtower 可以监听镜像更新并自动重启容器，但对数据库迁移、健康检查、回滚审批不够可控。当前项目含 Flyway 迁移和多端产物，不建议在现阶段引入。

## 11. 回滚步骤

假设上一个稳定标签是 `sha-old1234`：

```bash
cd /opt/aituan/app
cp deploy/.env deploy/.env.before-rollback
sed -i 's/^AITUAN_IMAGE_TAG=.*/AITUAN_IMAGE_TAG=sha-old1234/' deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml pull
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d --remove-orphans
curl -fsS http://127.0.0.1/actuator/health
```

如果错误来自数据库迁移，不能只回滚镜像；需要恢复部署前数据库备份或执行经过验证的补偿迁移。

## 12. 数据库迁移注意事项

CI 必须保留后端 `test` profile 验证，确保 Flyway 迁移脚本同时兼容：

- MySQL 8。
- H2 `MODE=MySQL` 空库迁移。

新增迁移时避免单方言特性，例如未经双端验证的 `AFTER`、触发器、存储过程、MySQL JSON 专有函数等。

生产部署前建议在 Actions 中增加一个“临时 MySQL 服务容器迁移测试”：

```yaml
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_DATABASE: aituan_test
      MYSQL_USER: aituan
      MYSQL_PASSWORD: aituan
      MYSQL_ROOT_PASSWORD: root
    ports:
      - 3306:3306
    options: >-
      --health-cmd="mysqladmin ping -h 127.0.0.1 -uaituan -paituan --silent"
      --health-interval=10s
      --health-timeout=5s
      --health-retries=12
```

然后用 MySQL datasource 跑一遍后端启动或迁移集成测试。

## 13. APK 在流水线中的处理建议

用户端 APK 构建耗时长、体积大，并且本项目要求每次更新交付 APK 都提升版本号和 build number。

建议策略：

| 场景 | 处理方式 |
| --- | --- |
| 每次 main 部署都需要 APK 下载 | Actions 构建 debug APK，复制到 `deploy/artifacts/downloads`，打进 `web` 镜像。简单但镜像会变大。 |
| 只在发版时更新 APK | 使用 `workflow_dispatch` 或 tag 发布单独构建 APK，上传为 GitHub Release Artifact 或服务器下载目录。更省资源。 |
| 课程演示临时部署 | 可以继续使用本地 `scripts/build/build_android_apk_server.ps1`，避免 CI 配 Android 环境。 |

若采用 CI 自动构建 APK，提交前必须确认 `apps/user_app/pubspec.yaml` 的 `version` 已递增。

### 13.1 已落地的手动 APK workflow

本次已新增独立 workflow：

```text
.github/workflows/android-apk.yml
```

触发方式：

```text
GitHub -> Actions -> aituan-android-apk -> Run workflow
```

输入项：

| 输入项 | 说明 |
| --- | --- |
| `api_origin` | 可选。临时覆盖 API Origin；留空时使用 GitHub Variable `SERVER_ORIGIN`。 |
| `upload_to_server` | 是否在打包成功后上传到服务器下载目录。默认 `false`。 |
| `apk_name` | APK 文件名，默认 `aituan-user-server-debug.apk`。只能是文件名，不能是路径。 |

关键规则：

- workflow 不写死 IP 或域名。
- `api_origin` 留空时读取 `SERVER_ORIGIN`；因此后续从 IP 切换到域名，只需修改 GitHub Variable `SERVER_ORIGIN` 后重新运行 APK workflow。
- `api_origin` 必须以 `http://` 或 `https://` 开头，不要包含 `/api`，末尾 `/` 会被自动去掉。
- 默认只上传 GitHub Actions Artifact，保留 14 天。
- `upload_to_server=true` 时，workflow 使用现有 `SERVER_HOST`、`SERVER_PORT`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_KNOWN_HOSTS` 连接服务器。
- 上传目录优先读取服务器 `/opt/aituan/app/deploy/.env` 中的 `AITUAN_DOWNLOADS_DIR`；如果没配置，则回退到 `/opt/aituan/data/downloads`。
- workflow 不修改本地现有 `scripts/build/build_android_apk_server.ps1`，本地打包方式仍可继续使用。

打包前仍需遵守项目规则：如果这是要交付给用户安装的新 APK，应先递增 `apps/user_app/pubspec.yaml` 中的 `version` 和 build number，例如从 `1.1.3+20` 递增到 `1.1.4+21`。

## 14. 安全建议

- SSH 部署私钥只授予部署用户，不使用个人主力私钥。
- 部署用户只允许访问 `/opt/aituan/app`、执行 Docker Compose，避免 root 全权限长期暴露。
- `SERVER_KNOWN_HOSTS` 使用服务器真实 host key，不在流水线中盲目 `StrictHostKeyChecking=no`。
- `deploy.yml` 默认只有手动 `workflow_dispatch` 会真正部署；如需 main push 自动部署，需要在 GitHub Variables 设置 `AUTO_DEPLOY_PRODUCTION=true`，且仍建议通过 `production` Environment 审批保护。
- 生产环境使用 GitHub Environment 审批，避免每次 push main 自动无确认上线。
- 镜像仓库如果设为私有，服务器使用只读 PAT 登录 GHCR。
- `.config`、`deploy/.env`、数据库备份、JWT secret、SMTP 授权码、第三方 Token 不进入镜像、不进入 Actions 日志。
- GitHub Actions 日志中不要打印完整 `.env` 和 `.config`。

## 15. 可行性风险与解决方案

| 风险 | 影响 | 解决方案 |
| --- | --- | --- |
| GitHub-hosted runner 的 Flutter 版本与本机不一致 | Flutter 构建失败或产物差异 | 固定 `flutter-version: "3.41.6"`；必要时改用自托管 runner。 |
| `deploy/.dockerignore` 排除 Web 产物 | Web 镜像缺文件 | 新增 Web 专用 context 或调整 `.dockerignore`。 |
| GHCR 私有镜像服务器拉取失败 | 部署失败 | 服务器一次性 `docker login ghcr.io`，使用只读 Token。 |
| 数据库迁移失败 | 后端启动失败，可能影响数据 | CI 加 MySQL/H2 双端验证；部署前备份数据库。 |
| APK 打进 Web 镜像导致镜像过大 | 拉取慢，占用带宽 | APK 改为 Release Artifact 或对象存储；只在发版时构建。 |
| SSH 部署命令误覆盖 `.env` | 配置丢失 | 部署脚本只修改 `AITUAN_IMAGE_REGISTRY`、`AITUAN_IMAGE_TAG` 和缺省的 `AITUAN_DOWNLOADS_DIR`，并先备份到 `/opt/aituan/backups/deploy.env.before-ci-<tag>`。 |
| 使用 `latest` 无法回滚 | 定位困难 | 固定 `sha-短提交号` 标签。 |

## 16. 分阶段落地计划

### 第一阶段：只做 CI 校验

新增 PR workflow：

- 后端测试。
- 商家端/后台端构建。
- Flutter analyze/test。

验收标准：PR 中任一检查失败时不能合并。

### 第二阶段：构建并推送镜像，但不自动部署

新增 main workflow：

- 测试通过后构建 `backend` / `web` 镜像。
- 推送 GHCR。
- 手动在服务器拉取指定 tag 验证。

验收标准：服务器能通过指定 tag 启动并访问所有入口。

### 第三阶段：Actions 自动部署到服务器

启用 production environment：

- main 分支构建镜像。
- 审批后 SSH 到服务器。
- 更新 `AITUAN_IMAGE_REGISTRY` 与 `AITUAN_IMAGE_TAG`。
- `docker compose pull && up -d`。
- 健康检查通过后流水线成功。

验收标准：GitHub Actions 日志可看到部署版本、服务器健康检查结果，失败时不会吞掉错误。

### 第四阶段：增强能力

- 加 MySQL service container 做迁移验证。
- 增加 GitHub Release / APK Artifact。
- 增加部署前数据库备份命令。
- 增加失败自动回滚到上一个 tag。
- 后续接入 HTTPS、域名、证书自动续期。

## 17. 官方资料参考

- Docker 官方 GitHub Actions 构建说明：<https://docs.docker.com/build/ci/github-actions/>
- GitHub 官方 Docker 镜像发布说明：<https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-docker-images>
- GitHub Container Registry 说明：<https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- Docker Compose `pull` 命令：<https://docs.docker.com/reference/cli/docker/compose/pull/>
- Docker Compose `up` 命令：<https://docs.docker.com/reference/cli/docker/compose/up/>
- Docker Compose `pull_policy`：<https://docs.docker.com/reference/compose-file/services/#pull_policy>
- GitHub Actions 部署与 Environments：<https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/control-deployments>
- GitHub Actions Secrets：<https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets>
