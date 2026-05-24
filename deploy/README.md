# 爱团服务器 Docker Compose 部署说明

本文说明后端 API、商家端 Web、后台端 Web 在 Debian 服务器上的 Compose 部署方式，以及如何构建对接服务器地址的三个前端产物。

## 1. 部署结构

服务器统一由 Nginx 暴露 80 端口：

| 地址 | 说明 |
| --- | --- |
| `http://182.92.238.178/merchant/` | 商家端 Web |
| `http://182.92.238.178/admin/` | 后台端 Web |
| `http://182.92.238.178/api/...` | 后端 API |
| `http://182.92.238.178/actuator/health` | 后端健康检查 |
| `http://182.92.238.178/downloads/aituan-user-server-debug.apk` | 用户端服务器版 APK 下载 |

Compose 服务包括：

- `mysql`：MySQL 8 数据库，不对公网暴露 3306。
- `backend`：Spring Boot 后端，仅在 Compose 内部网络暴露 8080。
- `nginx`：公网入口，托管两个 Web 静态产物并反向代理后端。

## 2. 敏感信息

不要把以下内容提交到 Git：

- SSH 密码、私钥。
- `deploy/.env`。
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

### 3.2 构建商家端和后台端服务器版 Web

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_frontends_server.ps1" -ServerOrigin "http://182.92.238.178"
```

输出：

```text
deploy/artifacts/merchant-web
deploy/artifacts/admin-web
```

构建参数说明：

- 商家端构建 base：`/merchant/`
- 后台端构建 base：`/admin/`
- API 地址：`http://182.92.238.178`

### 3.3 构建用户端服务器版 APK

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_android_apk_server.ps1" -ServerOrigin "http://182.92.238.178"
```

输出：

```text
D:/aituan_release/apk/aituan-user-server-debug.apk
deploy/artifacts/downloads/aituan-user-server-debug.apk
```

### 3.4 一次性构建全部服务器版产物

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "http://182.92.238.178"
```

如果只构建后端和两个 Web，不构建 APK：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/lixu/OneDrive/桌面/软工/new/scripts/build/build_all_server_artifacts.ps1" -ServerOrigin "http://182.92.238.178" -SkipApk
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

## 5. 创建服务器环境变量文件

在服务器中执行：

```bash
cd /opt/aituan/app/deploy
cp .env.example .env
```

编辑 `deploy/.env`，把占位值改为真实值：

```dotenv
MYSQL_DATABASE=aituan_dev
MYSQL_USER=aituan
MYSQL_PASSWORD=替换为强密码
MYSQL_ROOT_PASSWORD=替换为强密码
AITUAN_JWT_SECRET=替换为长随机字符串
AITUAN_DATA_DIR=/opt/aituan/data
```

可用以下命令生成 JWT secret：

```bash
openssl rand -base64 48
```

## 6. 启动服务

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

### 7.1 后端健康检查

```bash
curl http://182.92.238.178/actuator/health
```

预期返回健康状态。

### 7.2 商家端

浏览器访问：

```text
http://182.92.238.178/merchant/
```

检查浏览器 Network：API 请求应为 `http://182.92.238.178/api/...`，不应出现 `localhost:8080`。

### 7.3 后台端

浏览器访问：

```text
http://182.92.238.178/admin/
```

检查浏览器 Network：API 请求应为 `http://182.92.238.178/api/...`。

### 7.4 用户端 APK

下载安装：

```text
http://182.92.238.178/downloads/aituan-user-server-debug.apk
```

安装后验证登录、首页、订单等接口能访问服务器。

## 8. 2C2G 服务器注意事项

- 不建议在服务器上运行 Maven、npm、Flutter 构建任务。
- MySQL 已按小内存场景限制连接数和缓冲池。
- 后端 JVM 默认限制为 `-Xmx512m`。
- 如遇 OOM，可先检查 `docker stats`，必要时增加 swap 或升级服务器配置。
- 阿里云安全组至少需要放行 80 端口。

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
