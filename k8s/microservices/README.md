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

GitHub Actions 会从 Production Secrets 生成这些对象。`aituan-app-config` 的内容对应仓库根目录 `.config.example`，不再增加其他业务配置文件。四个业务数据库密码分别使用 `K8S_IDENTITY_DB_PASSWORD`、`K8S_MERCHANT_DB_PASSWORD`、`K8S_TRADE_DB_PASSWORD`、`K8S_PLATFORM_DB_PASSWORD`，不要复用同一密码。

`aituan-downloads` 是绑定服务器 `/opt/aituan/data/downloads` 的静态 hostPath PV/PVC。这样 APK 工作流通过 SSH 上传到宿主机后，K8s Web 能直接读取同一文件；该设计只适用于当前单节点 k3s。

Web 同时只读挂载宿主机 `/var/www/certbot`，用于 HTTP-01 challenge。Let's Encrypt 续期后执行 `scripts/deploy/sync_k8s_tls.sh`，把新证书同步到 `aituan-tls` 并滚动重启 Web。

## 发布和验证

manifest 中的 `sha-placeholder` 仅是占位值。发布时必须在 apply 前渲染为同一提交的不可变 SHA tag，覆盖 5 个后端镜像、MySQL 和 Web 镜像。GitHub Actions 会先完成替换并检查没有占位值，再一次性 apply，避免占位镜像短暂拉取失败。

```bash
kubectl -n aituan rollout status statefulset/mysql --timeout=600s
for service in identity-asset-service merchant-catalog-service trade-fulfillment-service engagement-platform-service api-gateway aituan-web; do
  kubectl -n aituan rollout status "deployment/${service}" --timeout=600s
done
kubectl -n aituan get pods,svc,hpa,pvc
curl -fsS https://aituan.2b.gs/actuator/health
```

资源请求仍保持课程验收所需的轻量配置；4C16G 单节点有充足余量。Gateway 与四个 Java 服务默认 `-Xmx256m`，MySQL buffer pool 为 128 MiB。

Gateway 与 A/B/C/D 五个 Java Deployment 均启用 `autoscaling/v2` HPA：CPU 目标 50%，最少 1、最多 4 个 Pod，扩容无稳定窗口，降容稳定窗口 120 秒。HPA 使用率以 Deployment 中的 CPU request 为分母，因此不可删除各服务的 `resources.requests.cpu`。生产实压与自动恢复实验使用：

```bash
HPA_LOAD_DURATION_SECONDS=180 HPA_LOAD_CONCURRENCY=80 \
  bash scripts/experiments/run_hpa_experiment.sh
```

脚本同时对 A/B/C/D 的只读接口施压（全部经过 Gateway），保存逐请求、HPA 副本数、Pod CPU/内存和节点配置，并要求至少两个业务微服务实际扩容且最终五个服务均缩回 1 Pod。

商品服务依赖故障实验使用：

```bash
bash scripts/experiments/run_catalog_fault_experiment.sh
```

脚本会短暂停止 B，验证 C 读取持久化购物车快照、拒绝依赖商品实时数据的写操作、仍允许移除/清空，并验证 Gateway/A/C/D 不随 B 崩溃；无论实验成功或中断，退出清理都会恢复 B 的 HPA 绑定和至少 1 个副本。完整口径见 `docs/stage-new-4/HPA与依赖故障实验说明.md`。
