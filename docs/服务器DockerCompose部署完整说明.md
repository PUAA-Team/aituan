# 爱团服务器 Docker Compose 部署完整说明

> 历史说明：本文最初记录旧服务器 HTTP/IP 部署口径，旧地址仅作迁移参考。当前生产/演示部署以 `aituan-new`、`8.220.192.106`、`https://aituan.2b.gs` 和 `docs/stage-new-1/新服务器Docker部署与CICD说明.md` 为准。真实密码、JWT secret、邮箱授权码、图床 Token 等敏感信息必须只保存在服务器本地，不得写入 Git、文档或聊天记录。

## 1. 当前部署目标

服务器统一由 Nginx 暴露公网 80 端口，后端与数据库仅在 Compose 内部网络访问。

| 入口 | 说明 |
| --- | --- |
| `http://182.92.238.178/` | 根路径下载展示页 |
| `http://182.92.238.178/web/` | 用户端 Flutter Web |
| `http://182.92.238.178/merchant/` | 商家端 Web |
| `http://182.92.238.178/admin/` | 后台端 Web |
| `http://182.92.238.178/api/...` | 后端 API，Nginx 反向代理到后端容器 |
| `http://182.92.238.178/actuator/health` | 后端健康检查 |
| `http://182.92.238.178/downloads/aituan-user-server-debug.apk` | 用户端服务器版 APK 下载 |

## 2. Compose 服务结构

当前 Compose 文件：`deploy/docker-compose.server.yml`。

| 服务 | 镜像/构建 | 暴露方式 | 持久化 | 说明 |
| --- | --- | --- | --- | --- |
| `mysql` | `mysql:8.0` | 仅 Compose 内部网络 | `${AITUAN_DATA_DIR}/mysql:/var/lib/mysql` | MySQL 8，设置 utf8mb4、上海时区、小内存参数；不对公网暴露 3306。 |
| `backend` | 从 `deploy/backend/Dockerfile` 构建 `aituan-backend:server` | 仅内部 `8080` | `${AITUAN_DATA_DIR}/uploads:/data/uploads`，`${AITUAN_CONFIG_HOST_FILE}:/app/.config:ro` | Spring Boot 后端，等待 MySQL 健康后启动。 |
| `nginx` | `nginx:1.27-alpine` | 公网 `80:80` | 挂载 `deploy/artifacts/*` 静态产物 | 负责静态页面、Web 端、APK 下载和 API 反向代理。 |

内部网络名为 `aituan-network`。后端通过容器名 `mysql` 访问数据库，Nginx 通过容器名 `backend` 代理接口。

## 3. 服务器目录规划

推荐服务器目录：

```text
/opt/aituan/app                  # 仓库代码、deploy 目录和部署产物
/opt/aituan/app/.config          # 后端业务敏感配置，不提交
/opt/aituan/app/deploy/.env      # Docker Compose 环境变量，不提交
/opt/aituan/data/mysql           # MySQL 数据卷
/opt/aituan/data/uploads         # 后端本地上传文件
/opt/aituan/backups              # 每次部署前备份
```

本地 Windows 构建缓存与产物按项目约定放在 D 盘，避免占用 C 盘：

| 用途 | 路径 |
| --- | --- |
| Maven 缓存 | `D:/aituan_cache/m2/` |
| npm 缓存 | `D:/aituan_cache/npm/` |
| Flutter Pub 缓存 | `D:/aituan_cache/pub/` |
| Gradle 缓存 | `D:/aituan_cache/gradle/` |
| 本地服务器版 APK 成品 | `D:/aituan_release/apk/aituan-user-server-debug.apk` |
| 部署用后端 JAR | `deploy/artifacts/backend/aituan-backend.jar` |
| 部署用静态产物 | `deploy/artifacts/landing`、`user-web`、`merchant-web`、`admin-web`、`downloads` |

## 4. 敏感配置文件

服务器需要两个不入仓库的配置文件。

### 4.1 `deploy/.env`

模板：`deploy/.env.example`。

示例：

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=aituan
MYSQL_PASSWORD=替换为强密码
MYSQL_ROOT_PASSWORD=替换为强密码
AITUAN_DATA_DIR=/opt/aituan/data
AITUAN_CONFIG_HOST_FILE=../.config
```

变量说明：

| 变量 | 用途 |
| --- | --- |
| `MYSQL_DATABASE` | Compose 启动时创建的业务库名。 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 后端连接 MySQL 的业务账号密码。 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，只保存在服务器。 |
| `AITUAN_DATA_DIR` | MySQL 与上传目录的宿主机根目录。 |
| `AITUAN_CONFIG_HOST_FILE` | 后端容器内 `/app/.config` 对应的宿主机配置文件路径。 |

### 4.2 `.config`

模板：`.config.example`。

至少要配置强随机 JWT secret：

```properties
aituan.security.jwt-secret=替换为长随机字符串
```

如服务器要发送邮箱验证码，继续配置 SMTP；如使用图床或高德地图，也在 `.config` 中配置对应 Token / Key。真实值不得提交。

生成 JWT secret 示例：

```bash
openssl rand -base64 48
```

## 5. 本地构建服务器版产物

> 建议在本机完成 Maven、npm、Flutter 构建后上传产物。2C2G 服务器不建议承担完整构建任务。

### 5.1 一次性构建全部服务器版产物

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "http://182.92.238.178"
```

如果本次不需要重新打 APK：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "http://182.92.238.178" -SkipApk
```

### 5.2 单独构建后端 JAR

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_backend_server.ps1"
```

输出：

```text
deploy/artifacts/backend/aituan-backend.jar
```

当前脚本为服务器版打包使用 `-DskipTests`，正式部署前仍应单独跑后端测试：

```powershell
$env:JAVA_HOME='D:/tools/jdk-17.0.18+8'; $env:Path="D:/tools/jdk-17.0.18+8/bin;$env:Path"; mvn -f services/backend/pom.xml -Dmaven.repo.local=D:/aituan_cache/m2 test
```

### 5.3 单独构建服务器版 Web

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_frontends_server.ps1" -ServerOrigin "http://182.92.238.178"
```

输出：

```text
deploy/artifacts/landing
deploy/artifacts/user-web
deploy/artifacts/merchant-web
deploy/artifacts/admin-web
```

构建口径：

| 端 | base / API |
| --- | --- |
| 根路径展示页 | 从 `deploy/landing` 同步到 `deploy/artifacts/landing`。 |
| 用户端 Web | `--base-href /web/`，`API_BASE_URL=http://182.92.238.178`。 |
| 商家端 Web | Vite base `/merchant/`，`VITE_API_BASE_URL=http://182.92.238.178`。 |
| 后台端 Web | Vite base `/admin/`，`VITE_API_BASE_URL=http://182.92.238.178`。 |

### 5.4 单独构建服务器版 APK

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_android_apk_server.ps1" -ServerOrigin "http://182.92.238.178"
```

输出：

```text
D:/aituan_release/apk/aituan-user-server-debug.apk
deploy/artifacts/downloads/aituan-user-server-debug.apk
```

## 6. 上传到服务器

推荐部署前先在服务器备份旧产物与 Compose 配置：

```bash
stamp=$(date +%Y%m%d-%H%M%S)
mkdir -p /opt/aituan/backups/$stamp
tar -czf /opt/aituan/backups/$stamp/deploy-before-update.tar.gz -C /opt/aituan/app deploy .config --exclude='deploy/.env'
cp /opt/aituan/app/deploy/.env /opt/aituan/backups/$stamp/deploy.env.backup
```

上传内容至少包括：

```text
deploy/docker-compose.server.yml
deploy/backend/Dockerfile
deploy/nginx/conf.d/default.conf
deploy/artifacts/backend/aituan-backend.jar
deploy/artifacts/landing
deploy/artifacts/user-web
deploy/artifacts/merchant-web
deploy/artifacts/admin-web
deploy/artifacts/downloads
```

注意：不要覆盖服务器真实的 `/opt/aituan/app/.config` 和 `/opt/aituan/app/deploy/.env`。

## 7. 首次启动或更新启动

在服务器执行：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml config
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

说明：

- `config` 用于展开变量并检查 Compose 文件是否有效。
- `up -d --build` 会按当前 `deploy/backend/Dockerfile` 和 `deploy/artifacts/backend/aituan-backend.jar` 重建后端镜像，并后台启动服务。
- `mysql` 数据和上传文件在 `/opt/aituan/data`，正常更新不会删除。

如果只更新静态产物但发现 Nginx 仍挂载旧目录，可强制重建 Nginx 容器：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --force-recreate nginx
```

## 8. 验证清单

### 8.1 容器状态

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml ps
```

预期：`mysql`、`backend`、`nginx` 均为运行状态，MySQL 健康检查通过。

### 8.2 健康检查

```bash
curl -i http://182.92.238.178/actuator/health
```

预期返回 `200` 和健康状态，例如：

```json
{"status":"UP"}
```

### 8.3 页面入口

逐个访问：

```text
http://182.92.238.178/
http://182.92.238.178/web/
http://182.92.238.178/merchant/
http://182.92.238.178/admin/
http://182.92.238.178/downloads/aituan-user-server-debug.apk
```

浏览器 Network 中，商家端和后台端接口应请求 `http://182.92.238.178/api/...`，不应出现 `localhost:8080`。

### 8.4 数据库迁移

后端启动时 Flyway 会执行 `database/migrations` 与 seed。迁移脚本必须同时兼容 MySQL 8 和 H2 `MODE=MySQL`；部署前至少运行后端测试 profile 构建验证。

## 9. 常用维护命令

查看日志：

```bash
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml logs -f backend
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml logs -f nginx
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml logs -f mysql
```

停止服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml down
```

更新后重启：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

查看资源：

```bash
docker stats
```

进入 MySQL 容器排查：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml exec mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"
```

## 10. 回滚方案

### 10.1 只回滚部署产物

如果更新后页面或后端异常，可恢复部署前备份：

```bash
cd /opt/aituan/app
# 先停止业务容器，避免文件被读写中覆盖
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml down
# 将下面路径替换为实际备份目录
tar -xzf /opt/aituan/backups/20260608-xxxxxx/deploy-before-update.tar.gz -C /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

### 10.2 数据库回滚

数据库迁移一旦执行，不建议直接手工删表或改 Flyway 记录。正确做法是：

1. 部署前先备份 MySQL 数据卷或导出 SQL。
2. 发现迁移错误后停止服务。
3. 恢复部署前数据库备份。
4. 修正迁移脚本后重新部署。

课程演示环境若允许丢弃演示数据，也仍必须保证迁移脚本能从空库完整初始化。

## 11. 常见问题排查

| 现象 | 优先排查 |
| --- | --- |
| `502 Bad Gateway` | 后端容器是否启动完成；`logs -f backend` 是否有 Flyway、数据库连接、`.config` 配置错误。 |
| 页面能打开但接口请求失败 | 浏览器 Network 中 API 地址是否仍是 `localhost`；确认服务器版 Web 构建时传入了正确 `ServerOrigin`。 |
| APK 调接口失败 | APK 是否为 `aituan-user-server-debug.apk`；构建时是否传入 `--dart-define=API_BASE_URL=http://182.92.238.178`。 |
| MySQL 起不来 | `deploy/.env` 密码是否为空；数据卷权限是否异常；服务器磁盘是否满。 |
| 上传图片失败 | `${AITUAN_DATA_DIR}/uploads` 是否挂载；`.config` 的上传策略和图床 Token 是否正确。 |
| 服务器内存不足 | `docker stats` 查看 MySQL、后端、Nginx；当前 Compose 已限制 MySQL 缓冲与 JVM `-Xmx512m`，仍不足时加 swap 或升级配置。 |

## 12. 安全与发布注意事项

- `deploy/.env`、`.config`、SSH 密钥、MySQL 密码、JWT secret、第三方 Token 不得提交。
- 演示账号只适合课程验收和演示，公开部署前应删除、禁用或修改默认密码。
- 服务器安全组至少开放 80 端口；如后续启用 HTTPS，再开放 443 并补充证书自动续期方案。
- 服务器上不要长期保留明文备份；需要保留时放在受限目录并定期清理。
- 更新 Compose、数据库迁移、线上配置前必须先备份并确认回滚方式。

## 13. 与后续 CI/CD 的关系

当前部署方式是“本地构建产物 + 上传服务器 + Compose 本机构建后端镜像 + Nginx 挂载静态目录”。它已经适合手动部署，但不完全等同于“服务器自动拉取更新镜像”。

如果后续要实现标准 CI/CD，建议演进为：

1. GitHub Actions 自动运行后端、Web、Flutter 测试。
2. Actions 构建后端镜像和前端静态站点镜像，推送到 GitHub Container Registry 或 Docker Hub。
3. 服务器 Compose 文件改为引用远程镜像，例如 `ghcr.io/<owner>/<repo>/backend:<tag>` 和 `ghcr.io/<owner>/<repo>/web:<tag>`。
4. 部署时服务器执行 `docker compose pull && docker compose up -d`，由 Compose 自动拉取新镜像并重建变化的容器。

详细可行性和步骤见 `docs/stage7/流水线部署与CICD可行性方案.md`。
