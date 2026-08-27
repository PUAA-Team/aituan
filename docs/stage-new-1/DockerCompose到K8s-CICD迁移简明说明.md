# Docker Compose 到 Kubernetes CI/CD 迁移简明说明

## 1. 为什么要从 Compose 改到 K8s

课程 C03 要求“push 后部署到 Kubernetes 并健康检查”，C04 要求“镜像不能只用 latest”。原来的 Docker Compose 部署已经能跑通线上服务，但部署动作是 GitHub Actions 通过 SSH 登录服务器后执行 `docker compose up -d`，不属于 Kubernetes 部署链路。

因此本次改造目标是：

- 保留测试、构建、推送镜像流程。
- 部署阶段从 SSH + Docker Compose 改为 `kubectl apply` + `kubectl rollout status`。
- 镜像部署统一使用 `sha-短提交号`，不用 `latest`。
- Compose 只作为回退方案，不再和 K8s 同机同时运行。

## 2. 原 Docker Compose CI/CD 流程

原流程如下：

```text
push main / 手动触发
  -> GitHub Actions 运行后端、Web、Flutter 测试
  -> 构建 backend 镜像和 web 镜像
  -> 推送到 GHCR
  -> SSH 登录服务器
  -> 写入 deploy/.env 中的镜像 tag
  -> docker compose pull
  -> docker compose up -d
  -> curl 健康检查
```

这种方式简单稳定，但课程验收中缺少 Kubernetes 资源、`kubectl apply` 和 `kubectl rollout status`。

## 3. 新 Kubernetes CI/CD 流程

新流程如下：

```text
push main / 手动触发
  -> GitHub Actions 运行后端、Web、Flutter 测试
  -> 构建 backend 镜像和 web 镜像
  -> 推送到 GHCR，tag 为 sha-短提交号
  -> 写入 KUBE_CONFIG
  -> kubectl apply -f k8s/
  -> kubectl set image 使用本次 sha tag
  -> kubectl rollout status 等待 MySQL/backend/web 发布成功
  -> kubectl get pods,svc,ingress 输出验收信息
```

关键变化：

| 项目 | Compose 方式 | K8s 方式 |
| --- | --- | --- |
| 连接服务器 | SSH | kubeconfig 连接 Kubernetes API |
| 部署命令 | `docker compose up -d` | `kubectl apply` / `kubectl set image` |
| 健康检查 | `curl` | `kubectl rollout status` + `curl` |
| 镜像版本 | `sha-短提交号` | `sha-短提交号` |
| 资源描述 | `deploy/docker-compose.cicd.yml` | `k8s/*.yaml` |

## 4. GitHub Actions 怎么切换

`.github/workflows/deploy.yml` 已支持 `deploy_target`：

```text
k8s     部署到 Kubernetes
compose 回退到 Docker Compose
none    只构建和推送镜像，不部署
```

push main 自动部署时看 GitHub Repository Variable：

```text
DEPLOY_TARGET=k8s
```

如果临时要回退 Compose，可以改成：

```text
DEPLOY_TARGET=compose
```

手动运行 `aituan-deploy` 时，也可以在页面上选择 `deploy_target`。

## 5. K8s 需要的 GitHub 配置

Repository Variables：

```text
SERVER_ORIGIN=https://aituan.2b.gs
AUTO_DEPLOY_PRODUCTION=true
DEPLOY_TARGET=k8s
K8S_NAMESPACE=aituan
```

Production Secrets：

```text
KUBE_CONFIG=<k3s/k8s 集群 kubeconfig 内容>
K8S_MYSQL_USER=<MySQL 用户名>
K8S_MYSQL_PASSWORD=<MySQL 密码>
K8S_MYSQL_ROOT_PASSWORD=<MySQL root 密码>
K8S_APP_CONFIG=<后端 .config 完整内容>
```

如果 GHCR 镜像保持私有，还需要：

```text
GHCR_PULL_USERNAME=<GitHub 用户名或机器人账号>
GHCR_PULL_TOKEN=<具备 read:packages 权限的 PAT>
```

注意：这些值都不能提交到仓库。

## 6. 服务器当前运行方式

当前 `aituan-new` 服务器按课程 K8s 验收优先处理：

```text
Docker Compose：停止
Docker daemon：停止并禁用开机自启
k3s：运行
公网 80/443：由 K8s web Service 接管
```

因为服务器内存只有约 2G，不适合同机同时运行 Compose 和 K8s 两套 MySQL/backend/web。现在只保留 K8s，避免资源被两套服务重复占用。

## 7. K8s 对外暴露方式

当前服务器是单节点 k3s，安装时禁用了 Traefik，也没有额外安装 nginx-ingress。为减少额外依赖，`k8s/04-web.yaml` 中的 `web` Service 使用：

```yaml
type: LoadBalancer
```

k3s 内置 servicelb 会把服务暴露到服务器公网 80/443。这样课程验收时可以直接访问：

```text
https://aituan.2b.gs/
https://aituan.2b.gs/web/
https://aituan.2b.gs/merchant/
https://aituan.2b.gs/admin/
https://aituan.2b.gs/actuator/health
```

## 8. 常用验收命令

在服务器上检查 K8s：

```bash
kubectl -n aituan get pods,svc,ingress
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
```

公网健康检查：

```bash
curl -k https://aituan.2b.gs/actuator/health
```

预期返回：

```json
{"status":"UP","groups":["liveness","readiness"]}
```

## 9. 回退到 Compose 的方法

如果后续确实要回退 Docker Compose，需要先释放 K8s 对 80/443 的占用，再重新启用 Docker：

```bash
kubectl -n aituan patch service web -p '{"spec":{"type":"ClusterIP"}}'
systemctl enable docker docker.socket
systemctl start docker docker.socket
cd /opt/aituan/app
docker compose --env-file deploy/.env -f deploy/docker-compose.cicd.yml up -d
```

CI/CD 回退时把 GitHub Variable 改为：

```text
DEPLOY_TARGET=compose
```

但课程验收建议保持：

```text
DEPLOY_TARGET=k8s
```
