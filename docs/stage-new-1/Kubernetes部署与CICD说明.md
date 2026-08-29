# Kubernetes 部署与 CI/CD 补齐说明

## 1. 背景

课程清单中 C03 要求“push 后部署到 K8s 并健康检查”，C04 要求“镜像不能只用 latest”。项目原有生产链路为 GitHub Actions 构建镜像后，通过 SSH 登录服务器执行 Docker Compose。该链路已经可用，但不属于 Kubernetes 部署。

本次补齐 Kubernetes 部署能力：

- 新增 `k8s/` manifests。
- `aituan-deploy` 支持 `deploy_target=k8s`。
- K8s 部署使用 `sha-短提交号` 镜像 tag。
- 使用 `kubectl rollout status` 做发布健康检查。
- 保留 Docker Compose 作为回退部署方式。

## 2. Docker Compose 与 Kubernetes 的边界

| 部署方式 | 当前定位 | 入口命令 | 适用场景 |
| --- | --- | --- | --- |
| Docker Compose | 回退链路 / 单服务器稳定部署 | `docker compose up -d` | 当前服务器、快速恢复 |
| Kubernetes | 课程标准主部署链路 | `kubectl apply` / `kubectl rollout status` | K8s 集群、课程验收、云原生部署展示 |

二者不冲突，仓库同时保留：

```text
deploy/docker-compose.cicd.yml
k8s/*.yaml
```

默认建议 GitHub Actions 使用 K8s，必要时手动切回 Compose。

## 3. Kubernetes 资源结构

`k8s/` 目录包含：

| 文件 | 资源 | 说明 |
| --- | --- | --- |
| `00-namespace.yaml` | Namespace | 创建 `aituan` 命名空间 |
| `01-configmap.yaml` | ConfigMap | 保存非敏感配置 |
| `02-mysql.yaml` | StatefulSet / Service / PVC | 运行 MySQL 8 并持久化数据 |
| `03-backend.yaml` | Deployment / Service / PVC | 运行 Spring Boot 后端，配置健康检查 |
| `04-web.yaml` | Deployment / Service / PVC | 运行 Nginx/Web 镜像，单节点 k3s 下通过 LoadBalancer 暴露 80/443 |
| `05-ingress.yaml` | Ingress | 预留 Ingress Controller 场景，通过 `aituan.2b.gs` 访问 Web 入口 |
| `secret.example.yaml` | Secret 示例 | 仅说明字段，不保存真实密钥 |
| `README.md` | 操作说明 | 首次部署、Secrets、验证和回滚 |

## 4. GitHub Actions 部署目标

`.github/workflows/deploy.yml` 中增加 `deploy_target`：

```text
k8s     部署到 Kubernetes
compose 回退到 Docker Compose
none    只构建和推送镜像，不部署
```

手动运行 `aituan-deploy` 时可以选择部署目标。push main 自动部署时，由 GitHub Variable 控制：

```text
DEPLOY_TARGET=k8s
```

如果 K8s 集群暂不可用，可以临时改为：

```text
DEPLOY_TARGET=compose
```

## 5. GitHub 配置项

### 5.1 Repository Variables

```text
SERVER_ORIGIN=https://aituan.2b.gs
AUTO_DEPLOY_PRODUCTION=true
DEPLOY_TARGET=k8s
K8S_NAMESPACE=aituan
```

### 5.2 Production Secrets

K8s 部署需要：

```text
KUBE_CONFIG=<Kubernetes 集群 kubeconfig 内容>
K8S_MYSQL_USER=<MySQL 用户名>
K8S_MYSQL_PASSWORD=<MySQL 密码>
K8S_MYSQL_ROOT_PASSWORD=<MySQL root 密码>
K8S_APP_CONFIG=<后端 .config 完整内容>
```

如果 GHCR 镜像是私有包，还需要：

```text
GHCR_PULL_USERNAME=<GitHub 用户名或机器人账号>
GHCR_PULL_TOKEN=<具备 read:packages 权限的 PAT>
```

Compose 回退链路仍使用：

```text
SERVER_HOST
SERVER_PORT
SERVER_USER
SERVER_SSH_KEY
SERVER_KNOWN_HOSTS
```

## 6. C04 镜像版本控制

现有 workflow 计算镜像 tag：

```bash
image_tag="sha-${GITHUB_SHA::7}"
```

推送镜像：

```text
ghcr.io/puaa-team/aituan/backend:sha-xxxxxxx
ghcr.io/puaa-team/aituan/web:sha-xxxxxxx
```

K8s 部署时不使用 `latest`，而是执行：

```bash
kubectl -n aituan set image deployment/aituan-backend \
  backend=${IMAGE_REGISTRY}/backend:${IMAGE_TAG}

kubectl -n aituan set image deployment/aituan-web \
  web=${IMAGE_REGISTRY}/web:${IMAGE_TAG}
```

因此每次部署都能追踪到具体 commit，满足 C04。

## 7. C03 健康检查

K8s 部署后 Actions 等待：

```bash
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
```

同时 `backend.yaml` 中配置：

```text
readinessProbe: /actuator/health
livenessProbe: /actuator/health
```

`web.yaml` 中通过 Nginx 的 `/actuator/health` 反向代理检查后端状态。

## 8. HTTPS 与证书

当前 Web 镜像内 Nginx 模板使用：

```text
/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem
/etc/letsencrypt/live/aituan.2b.gs/privkey.pem
```

K8s 中通过 `aituan-tls` Secret 挂载，仓库不保存证书内容。

创建方式示例：

```bash
kubectl -n aituan create secret tls aituan-tls \
  --cert=/path/to/fullchain.pem \
  --key=/path/to/privkey.pem
```

如果集群已经安装 cert-manager，也可以让 cert-manager 自动维护 `aituan-tls`。

## 9. 验证方式

手动部署验证：

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-mysql.yaml
kubectl apply -f k8s/03-backend.yaml
kubectl apply -f k8s/04-web.yaml
kubectl apply -f k8s/05-ingress.yaml
```

当前 `aituan-new` 服务器采用单节点 k3s，未启用 Traefik，也未额外安装 nginx-ingress。为快速满足课程验收，`web` Service 使用 `LoadBalancer`，由 k3s 内置 servicelb 接管公网 80/443；因此同机不要同时运行 Docker Compose 的 Nginx。

查看状态：

```bash
kubectl -n aituan get pods,svc,ingress
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
```

公网验证：

```bash
curl -fsS https://aituan.2b.gs/actuator/health
```

浏览器验证：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
```

## 10. 流水线原始报告

`aituan-deploy` 每次运行结束后都会通过 `pipeline_raw_report` job 上传原始报告 artifact：

```text
aituan-deploy-pipeline-raw-report
```

报告中包含本次 run 的触发事件 payload、run 元数据、jobs 原始 JSON、提交 hash、checkout 状态，以及部署目标、镜像仓库、镜像标签、是否部署、K8s/Compose job 结果等关键信息。部署失败时该 job 使用 `if: always()` 尽量继续上传报告，便于追溯失败阶段。

详见：`docs/stage-new-1/GitHubActions流水线原始报告说明.md`。

## 11. 回滚

Kubernetes 内回滚：

```bash
kubectl -n aituan rollout undo deployment/aituan-backend
kubectl -n aituan rollout undo deployment/aituan-web
```

回退到 Docker Compose：

1. 手动运行 `aituan-deploy`。
2. 选择：

```text
deploy=true
deploy_target=compose
```

或者把 GitHub Variable 改为：

```text
DEPLOY_TARGET=compose
```

## 11. C03/C04 状态

| 编号 | 课程要求 | 补齐结果 |
| --- | --- | --- |
| C03 | push 后部署到 K8s 并健康检查 | `aituan-deploy` 支持 K8s 部署，执行 `kubectl apply` 和 `kubectl rollout status` |
| C04 | 镜像不能只用 latest | K8s 使用 `IMAGE_TAG=sha-短提交号` 执行 `kubectl set image` |
