# C 分工测试容器化与流水线说明

## 1. 范围说明

本文记录 C 分工 `trade-fulfillment-service` 能独立完成的测试、Docker、K8s 与 CI 准备工作。

本阶段只保证 C 服务具备“可独立测试、可独立打包、可独立构建镜像、可用 K8s YAML 声明部署”的能力，不替代全员后续的微服务总集成。

## 2. 本阶段已补齐内容

| 类型 | 文件 | 说明 |
| --- | --- | --- |
| 测试工具 | `services/trade-fulfillment-service/src/test/java/com/aituan/ApiTestSupport.java` | 增加业务错误、403、400 等统一断言 |
| C 服务 API 测试 | `services/trade-fulfillment-service/src/test/java/com/aituan/tradefulfillment/TradeOrderDetailApiIntegrationTest.java` | 覆盖用户订单详情 |
| C 服务操作测试 | `services/trade-fulfillment-service/src/test/java/com/aituan/tradefulfillment/TradeOrderActionApiIntegrationTest.java` | 覆盖取消、催单、修改配送地址 |
| C 服务鉴权测试 | `services/trade-fulfillment-service/src/test/java/com/aituan/tradefulfillment/TradeSecurityApiIntegrationTest.java` | 覆盖 401、403、参数校验 |
| C 服务负向测试 | `services/trade-fulfillment-service/src/test/java/com/aituan/tradefulfillment/TradeOpsNegativeApiIntegrationTest.java` | 覆盖商家/后台履约负向场景 |
| MySQL 迁移测试 | `services/trade-fulfillment-service/src/test/java/com/aituan/tradefulfillment/TradeFulfillmentMysqlMigrationSmokeTest.java` | CI 显式开启后验证 MySQL 8 空库迁移 |
| Gateway 路由测试 | `services/api-gateway/src/test/java/com/aituan/gateway/GatewayTradeRouteTest.java` | 验证 trade 路由指向 C 服务且无 legacy fallback |
| 本地测试脚本 | `scripts/verify/test_trade_fulfillment.ps1` | 使用 D 盘 Maven 缓存运行 C 服务测试 |
| 本地打包脚本 | `scripts/build/build_trade_fulfillment_server.ps1` | 输出 C 服务 jar 到部署 artifacts 目录 |
| Dockerfile | `deploy/microservices/trade-fulfillment-service/Dockerfile` | 构建 C 服务独立镜像 |
| K8s YAML | `k8s/12-trade-fulfillment-service.yaml` | 声明 C 服务 Deployment 与 Service |
| CI workflow | `.github/workflows/trade-fulfillment-ci.yml` | 独立运行 C 服务测试、迁移冒烟、路由测试和镜像构建 |

## 3. 本地测试

默认使用 H2 `MODE=MySQL`，不依赖 Docker Compose，不连接其他服务数据库。

```powershell
& "scripts/verify/test_trade_fulfillment.ps1"
```

脚本会执行：

1. `trade-fulfillment-service` H2 集成测试。
2. Gateway trade 路由测试。
3. 默认跳过 MySQL 迁移冒烟。

如需本地显式运行 MySQL 迁移冒烟，需要先准备 MySQL 8 空库并设置环境变量：

```powershell
$env:TRADE_DB_URL="jdbc:mysql://127.0.0.1:3306/aituan_trade?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:TRADE_DB_USERNAME="aituan_trade"
$env:TRADE_DB_PASSWORD="本地测试密码"
& "scripts/verify/test_trade_fulfillment.ps1" -IncludeMysqlSmoke
```

注意：真实数据库密码不要写入仓库。

## 4. 本地打包

```powershell
& "scripts/build/build_trade_fulfillment_server.ps1"
```

输出：

```text
deploy/artifacts/trade-fulfillment-service/aituan-trade-fulfillment-service.jar
```

脚本使用：

- Maven 本地仓库：`D:/aituan_cache/m2`
- Java 17：优先使用 `D:/tools/jdk-17.0.18+8`

## 5. Docker 镜像

Dockerfile：

```text
deploy/microservices/trade-fulfillment-service/Dockerfile
```

构建前先运行打包脚本，保证 jar 已复制到 `deploy/artifacts/trade-fulfillment-service/`。

```powershell
docker build -f "deploy/microservices/trade-fulfillment-service/Dockerfile" -t aituan/trade-fulfillment-service:local "."
```

本机 C 盘空间紧张时，不建议频繁本地 Docker build；优先让 GitHub Actions 中的 Docker build job 验证镜像构建。

Dockerfile 不包含任何真实数据库地址、数据库密码、JWT Secret 或生产域名，运行时通过环境变量注入。

## 6. Kubernetes YAML

C 服务声明式部署文件：

```text
k8s/12-trade-fulfillment-service.yaml
```

包含：

- `Service/trade-fulfillment-service`
- `Deployment/aituan-trade-fulfillment-service`
- 端口 `8083`
- `/actuator/health` readiness/liveness probe
- CPU/内存 requests 和 limits
- `TRADE_DB_URL`、`TRADE_DB_USERNAME`、`TRADE_DB_PASSWORD`、`AITUAN_JWT_SECRET` 等环境变量

Secret 约束：

- `TRADE_DB_USERNAME`、`TRADE_DB_PASSWORD`、`AITUAN_JWT_SECRET` 通过 `aituan-trade-db-secret` 引用。
- 仓库内只保留引用，不提交真实 Secret。
- 真实部署前必须确认 `aituan_trade` 数据库和对应账号权限已经创建。

只做语法 dry-run 时：

```powershell
kubectl apply --dry-run=client -f "k8s/12-trade-fulfillment-service.yaml"
```

禁止在未额外确认前执行真实部署命令：

```powershell
kubectl apply -f "k8s/12-trade-fulfillment-service.yaml"
kubectl set image ...
kubectl rollout restart ...
```

## 7. GitHub Actions CI

新增独立 workflow：

```text
.github/workflows/trade-fulfillment-ci.yml
```

触发方式：

- `pull_request`，仅 C 服务及相关路径变更时触发。
- `workflow_dispatch`，可手动触发。

Job：

1. `trade-tests`：运行 C 服务 H2 测试并上传 surefire 原始报告。
2. `trade-mysql-migration-smoke`：使用 GitHub Actions MySQL 8 service 验证 C 服务 Flyway 空库迁移。
3. `gateway-trade-route-test`：验证 Gateway trade 路由配置。
4. `trade-package-and-docker-build`：打包 C 服务 jar 并只构建 Docker 镜像，不 push。
5. `pipeline_raw_report`：复用 `scripts/verify/write_pipeline_raw_report.sh` 生成原始流水线报告。

上传的 artifact：

- `trade-fulfillment-surefire-reports`
- `trade-fulfillment-mysql-smoke-reports`
- `gateway-trade-route-test-reports`
- `trade-fulfillment-ci-pipeline-raw-report`

本 workflow 不修改现有 `.github/workflows/ci.yml`，不修改 `.github/workflows/deploy.yml`，不自动部署生产环境。

## 8. 与全员集成的边界

C 分工当前能证明：

- C 服务自己的接口和业务规则可测。
- C 服务自己的 11 张表能由 Flyway 创建。
- Gateway trade 路由配置已经指向 C 服务。
- C 服务可以被单独打包和构建镜像。
- C 服务有 K8s Deployment/Service 模板。

仍需全员集成后完成：

- Gateway、identity、merchant、trade、platform 五个运行服务的统一部署。
- 四个业务库及账号 Secret 的真实创建与授权。
- C 服务对 A/B/D 服务的真实 HTTP client 替换。
- 前端经 Gateway 调用微服务的 E2E 回归。
- GHCR 镜像 push、K8s rollout、HPA、故障处理和性能对比实验。

## 9. 暂停点

遇到以下情况必须停止并确认：

1. C 服务测试需要直接访问其他服务表才能通过。
2. H2 或 MySQL 8 空库迁移失败。
3. 需要提交真实数据库密码、JWT Secret、kubeconfig、GHCR token。
4. 需要真实执行 Kubernetes 部署。
5. 需要覆盖现有主部署流水线。
6. 需要 commit 或 push。
