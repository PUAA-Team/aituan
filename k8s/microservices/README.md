# 微服务 Kubernetes 部署

本目录是当前验收版生产拓扑的唯一 Kustomize 入口：

```bash
kubectl apply -k k8s/microservices
```

## 拓扑

- `api-gateway:8080`：唯一 API 入口；
- `identity-asset-service:8081`：A，只使用 `aituan_identity`；
- `merchant-catalog-service:8082`：B，只使用 `aituan_merchant`；
- `trade-fulfillment-service:8083`：C，只使用 `aituan_trade`；
- `engagement-platform-service:8084`：D，只使用 `aituan_platform`；
- `aituan-web` + `web` LoadBalancer：用户端、商家端、管理端静态资源和 80/443 公网入口；
- `mysql` StatefulSet：一个 MySQL 实例中的四个逻辑 schema，通过四个限权账号禁止跨库访问。

## 必需 Secret

Kustomize 不包含真实密钥。部署前必须存在：

```text
aituan-mysql-root-secret       password
aituan-identity-db-secret      username, password
aituan-merchant-db-secret      username, password
aituan-trade-db-secret         username, password
aituan-platform-db-secret      username, password
aituan-service-secret          jwt-secret, internal-service-token
aituan-app-config              .config
aituan-tls                     tls.crt, tls.key
ghcr-pull-secret               私有 GHCR 镜像需要
```

GitHub Actions 会从 Production Secrets 生成这些对象。`aituan-app-config` 的内容对应仓库根目录 `.config.example`，不再增加其他业务配置文件。

## 发布和验证

manifest 中的 `sha-placeholder` 仅是占位值。发布时必须使用同一提交的不可变 SHA tag 覆盖 5 个后端镜像和 Web 镜像。

```bash
kubectl -n aituan rollout status statefulset/mysql --timeout=600s
for service in identity-asset-service merchant-catalog-service trade-fulfillment-service engagement-platform-service api-gateway aituan-web; do
  kubectl -n aituan rollout status "deployment/${service}" --timeout=600s
done
kubectl -n aituan get pods,svc,hpa,pvc
curl -fsS https://aituan.2b.gs/actuator/health
```

服务器资源按 2 GiB 级别单节点 k3s 收紧：Gateway 与四个 Java 服务默认 `-Xmx256m`，MySQL buffer pool 为 128 MiB。HPA 保留在商家商品服务上，但扩容仍受单节点可分配资源限制。
