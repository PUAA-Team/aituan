# engagement-platform-service 镜像与数据库准备

构建上下文必须是 `services`，这样 Dockerfile 只接收 Maven 多模块工程：

```powershell
docker build -f deploy/microservices/engagement-platform-service/Dockerfile `
  -t aituan/engagement-platform-service:local services
```

首次部署前，由数据库管理员执行一次逻辑库与最小权限账号初始化；密码应与 Kubernetes Secret 一致，不写入仓库：

```sql
CREATE DATABASE IF NOT EXISTS aituan_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'aituan_platform_svc'@'%' IDENTIFIED BY '<从 Secret 读取的密码>';
GRANT ALL PRIVILEGES ON aituan_platform.* TO 'aituan_platform_svc'@'%';
```

服务启动时只连接 `aituan_platform`，Flyway 自动执行
`db/migration/platform/V1__platform_schema.sql` 和 `db/seed/platform/R__platform_seed.sql`。
业务账号不授予其他逻辑库权限。
