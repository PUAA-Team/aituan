# 爱团 Kubernetes 部署说明

本文说明课程标准下的 Kubernetes 部署链路。当前仓库仍保留 Docker Compose 作为回退方式，K8s 作为 CI/CD 默认部署目标时，需要先准备集群凭证和必要 Secret。

## 1. 部署结构

Kubernetes 资源统一部署到 `aituan` namespace：

| 文件 | 资源 | 作用 |
| --- | --- | --- |
| `00-namespace.yaml` | Namespace | 创建 `aituan` 命名空间 |
| `01-configmap.yaml` | ConfigMap | 保存非敏感配置 |
| `02-mysql.yaml` | StatefulSet / Service / PVC | 部署 MySQL 8 并持久化数据 |
| `03-backend.yaml` | Deployment / Service / PVC | 部署 Spring Boot 后端和上传目录 |
| `04-web.yaml` | Deployment / Service / PVC | 部署 Nginx/Web 入口和下载目录；单节点 k3s 下通过 LoadBalancer 接管 80/443 |
| `05-ingress.yaml` | Ingress | 如果后续安装 Ingress Controller，可通过 `aituan.2b.gs` 暴露 Web 入口 |
| `secret.example.yaml` | Secret 示例 | 说明需要哪些 Secret，不保存真实值 |

## 2. 不能提交的敏感信息

不要把以下内容提交到 Git：

- `KUBE_CONFIG` / kubeconfig。
- MySQL 密码。
- JWT secret。
- 邮箱授权码、地图 Key、AI Key、图床 Token。
- GitHub / GHCR Token。
- TLS 证书私钥。

仓库中的 `secret.example.yaml` 只作为字段示例，真实生产环境应通过 `kubectl create secret ...` 或 GitHub Actions Secrets 创建。

## 3. GitHub Actions 需要的配置

### 3.1 Repository Variables

建议在 GitHub Repository Variables 中设置：

```text
SERVER_ORIGIN=https://aituan.2b.gs
AUTO_DEPLOY_PRODUCTION=true
DEPLOY_TARGET=k8s
K8S_NAMESPACE=aituan
```

说明：

- `DEPLOY_TARGET=k8s`：push main 后默认部署到 Kubernetes。
- 如需临时回退 Docker Compose，可改为 `DEPLOY_TARGET=compose`，或手动运行 workflow 时选择 `deploy_target=compose`。

### 3.2 Production Secrets

K8s 部署至少需要：

```text
KUBE_CONFIG=<Kubernetes 集群 kubeconfig 内容>
K8S_MYSQL_USER=<MySQL 业务用户名>
K8S_MYSQL_PASSWORD=<MySQL 业务用户密码>
K8S_MYSQL_ROOT_PASSWORD=<MySQL root 密码>
K8S_APP_CONFIG=<后端 /app/.config 文件完整内容>
```

如果 GHCR 镜像是私有包，还需要：

```text
GHCR_PULL_USERNAME=<GitHub 用户名或机器人账号>
GHCR_PULL_TOKEN=<具备 read:packages 权限的 GitHub PAT>
```

Compose 回退链路仍使用原有 SSH Secrets：

```text
SERVER_HOST
SERVER_PORT
SERVER_USER
SERVER_SSH_KEY
SERVER_KNOWN_HOSTS
```

## 4. TLS Secret

当前 `web` 镜像内的 Nginx 模板读取以下路径：

```text
/etc/letsencrypt/live/aituan.2b.gs/fullchain.pem
/etc/letsencrypt/live/aituan.2b.gs/privkey.pem
```

K8s 中使用 `aituan-tls` Secret 挂载到上述路径，Secret 字段通过 `04-web.yaml` 映射为：

```text
tls.crt -> fullchain.pem
tls.key -> privkey.pem
```

可用已有证书创建：

```bash
kubectl -n aituan create secret tls aituan-tls \
  --cert=/path/to/fullchain.pem \
  --key=/path/to/privkey.pem
```

也可以改用 cert-manager 自动签发，后续只要保证生成的 Secret 名为 `aituan-tls` 即可。

## 5. 手动部署验证

首次手动验证时，先创建 namespace：

```bash
kubectl apply -f k8s/00-namespace.yaml
```

再准备数据库和后端配置 Secret。示例：

```bash
kubectl -n aituan create secret generic aituan-db-secret \
  --from-literal=MYSQL_USER='替换为用户名' \
  --from-literal=MYSQL_PASSWORD='替换为密码' \
  --from-literal=MYSQL_ROOT_PASSWORD='替换为root密码'

kubectl -n aituan create secret generic aituan-app-config \
  --from-file=.config=/path/to/.config
```

如 GHCR 私有镜像需要拉取 Secret：

```bash
kubectl -n aituan create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username='<GitHub用户名>' \
  --docker-password='<read:packages token>'
```

应用资源：

```bash
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-mysql.yaml
kubectl apply -f k8s/03-backend.yaml
kubectl apply -f k8s/04-web.yaml
kubectl apply -f k8s/05-ingress.yaml
```

当前服务器使用单节点 k3s，`04-web.yaml` 中的 `web` Service 类型为 `LoadBalancer`，由 k3s 内置 servicelb 直接接管公网 80/443。为了避免端口冲突，同机部署时不要同时运行 Docker Compose 的 `nginx` 服务。

部署指定版本镜像，注意使用 sha tag：

```bash
kubectl -n aituan set image deployment/aituan-backend \
  backend=ghcr.io/puaa-team/aituan/backend:sha-xxxxxxx

kubectl -n aituan set image deployment/aituan-web \
  web=ghcr.io/puaa-team/aituan/web:sha-xxxxxxx
```

## 6. 健康检查

```bash
kubectl -n aituan rollout status statefulset/mysql --timeout=300s
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
kubectl -n aituan get pods,svc,ingress
```

集群内检查后端：

```bash
kubectl -n aituan run curl-smoke --rm -i --restart=Never \
  --image=curlimages/curl:8.10.1 -- \
  curl -fsS http://backend:8080/actuator/health
```

公网检查：

```bash
curl -fsS https://aituan.2b.gs/actuator/health
```

GitHub Actions 的 K8s 部署流程会在 `kubectl rollout status` 之后继续执行应用健康检查：先在集群内用临时 `curlimages/curl` Pod 请求 `http://backend:8080/actuator/health`，再从 Actions Runner 请求 `${SERVER_ORIGIN}/actuator/health`。两个检查都带 24 次重试和 5 秒等待，避免 Pod、Service 或 Ingress 刚发布完成但应用还未完全稳定时误判失败。

## 7. GitHub Actions 部署

手动运行 `aituan-deploy`：

- `deploy=true`
- `deploy_target=k8s`

Actions 会执行：

1. 构建并推送 `backend:sha-短提交号` 和 `web:sha-短提交号`。
2. 写入 `KUBE_CONFIG`。
3. 创建或复用 K8s Secret。
4. `kubectl apply` 所有 manifest。
5. `kubectl set image` 切换到本次 sha tag。
6. `kubectl rollout status` 等待发布成功。
7. 执行集群内后端健康检查和公网 `/actuator/health` 重试检查。

## 8. 回滚

### 8.1 Kubernetes 内回滚

```bash
kubectl -n aituan rollout undo deployment/aituan-backend
kubectl -n aituan rollout undo deployment/aituan-web
kubectl -n aituan rollout status deployment/aituan-backend --timeout=300s
kubectl -n aituan rollout status deployment/aituan-web --timeout=300s
```

也可以指定上一版 sha tag：

```bash
kubectl -n aituan set image deployment/aituan-backend backend=ghcr.io/puaa-team/aituan/backend:sha-old
kubectl -n aituan set image deployment/aituan-web web=ghcr.io/puaa-team/aituan/web:sha-old
```

### 8.2 回退到 Docker Compose

手动运行 `aituan-deploy`，选择：

```text
deploy=true
deploy_target=compose
```

或把 GitHub Variable 改为：

```text
DEPLOY_TARGET=compose
```

即可走原 SSH + Docker Compose 链路。