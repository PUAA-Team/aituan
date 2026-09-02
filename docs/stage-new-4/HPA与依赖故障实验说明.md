# HPA 与依赖故障实验说明

> 适用拓扑：生产 k3s namespace `aituan`，Gateway + A/B/C/D 四个业务微服务
> 对应课程要求：云原生扩缩容实验、依赖故障处理与恢复实验

## 1. 验收设计

### 1.1 多服务 HPA

以下五个 Java Deployment 均有独立 `autoscaling/v2` HPA，而非只给 B 增加一份演示配置：

| 工作负载 | CPU request | CPU 目标 | 副本范围 |
| --- | ---: | ---: | ---: |
| `api-gateway` | 50m | 50% | 1–4 |
| `identity-asset-service`（A） | 75m | 50% | 1–4 |
| `merchant-catalog-service`（B） | 75m | 50% | 1–4 |
| `trade-fulfillment-service`（C） | 75m | 50% | 1–4 |
| `engagement-platform-service`（D） | 75m | 50% | 1–4 |

扩容立即计算，可按 15 秒周期增加 100% 或 2 个 Pod；降容设置 120 秒稳定窗口并分批下降，避免流量抖动造成频繁伸缩。CD 在每次生产部署后检查 HPA 数量、目标绑定和 CPU 指标，不允许“清单存在但 metrics-server 不工作”。

`scripts/experiments/hpa-load.mjs` 按相同比例请求 A 用户资料、B 首页、C 订单列表和 D 门店评价，所有请求均经 Gateway。`scripts/experiments/run_hpa_experiment.sh` 在集群内创建临时 Node Job，并同步采集 HPA 副本、Pod CPU/内存、节点配置和 Kubernetes 事件。

实验通过条件：

1. 登录成功并产生真实业务请求，整体错误率不超过 5%；
2. A/B/C/D 中至少两个业务微服务的实际副本数大于 1；
3. 压力结束后，Gateway 与 A/B/C/D 最终全部恢复为 1 个副本；
4. 保存逐请求 JSONL、吞吐/平均/P95/P99、资源时间线和集群配置。

## 2. 商品服务故障时的购物车策略

C 拥有购物车，B 拥有商品、实时价格和库存。C 不跨库读取 B，而是在正常加入/修改购物车时，把展示所需字段保存到自己的 `cart` / `cart_item` 快照列中。该快照在 MySQL 中持久化，能被 C 的多个 Pod 共享，也不会因 C 重启或 HPA 换 Pod 丢失。

| 操作 | B 正常 | B 故障 | 原因 |
| --- | --- | --- | --- |
| 查询购物车 | B 实时数据并刷新快照 | 返回 C 的最近快照，标记 `catalogAvailable=false` 并给出提示 | 用户仍能看到已有购物车 |
| 新增商品 | 成功 | 2 秒有界超时内返回 9999 | 不能在未知实时价格/库存时接收新写入 |
| 修改数量 | 成功 | 2 秒有界超时内返回 9999 | 同上，且校验发生在数据库修改前 |
| 移除商品 | 成功 | 成功 | 只修改 C 自己拥有的购物车数据 |
| 清空购物车 | 成功 | 成功 | 只修改 C 自己拥有的购物车数据 |
| 下单/结算 | 正常跨服务校验 | 明确失败，不执行后续库存/优惠券动作 | 避免部分成功和错误订单 |

C 到 B 的连接超时为 1 秒、读取超时为 2 秒，故障不会无限占用请求线程。C 的健康探针不把 B 当作自身存活条件，因此 B 退出不会导致 Kubernetes 重启 C；Gateway、A、C、D 可继续服务各自不依赖 B 的功能。B 恢复后下一次请求重新使用实时数据并刷新快照，无需重启 C。

`TradeCatalogFailureIsolationIntegrationTest` 自动覆盖“正常建快照 → B 故障 → 降级读取 → 写失败且无部分写入 → 故障期间移除 → B 恢复后重新写入”的完整状态转换。Flyway `V003` 负责对现有购物车无损增加快照列。

## 3. 生产执行

运行前确认当前上下文是验收集群。两个脚本只在 namespace 内创建临时实验资源；故障脚本会短暂停止 B，并通过 `trap` 在成功、失败或中断时恢复 HPA 目标和 Deployment。

```bash
kubectl config current-context
kubectl -n aituan get pods,svc,hpa

HPA_LOAD_DURATION_SECONDS=180 \
HPA_LOAD_CONCURRENCY=80 \
bash scripts/experiments/run_hpa_experiment.sh

bash scripts/experiments/run_catalog_fault_experiment.sh
```

可用环境变量：

| 变量 | 默认值 | 作用 |
| --- | --- | --- |
| `K8S_NAMESPACE` | `aituan` | 实验 namespace |
| `HPA_LOAD_DURATION_SECONDS` | `180` | 加压时长 |
| `HPA_LOAD_CONCURRENCY` | `80` | 并发 worker 数 |
| `HPA_LOAD_ACCOUNT` / `HPA_LOAD_PASSWORD` | 默认演示用户 | 登录账号 |
| `HPA_EXPERIMENT_OUTPUT_DIR` | 带 UTC 时间的 stage-new-4 目录 | HPA 原始证据目录 |
| `FAULT_TEST_ORIGIN` | `https://aituan.2b.gs` | 故障实验的公开入口 |
| `FAULT_TEST_ACCOUNT` / `FAULT_TEST_PASSWORD` | 默认演示用户 | 登录账号 |
| `FAULT_EXPERIMENT_OUTPUT_DIR` | 带 UTC 时间的 stage-new-4 目录 | 故障原始证据目录 |

账号密码通过临时 Kubernetes Secret 传给压测 Job，输出不保存访问令牌或密码。两个脚本结束后会删除临时资源；故障实验还会清空自己的测试购物车。

## 4. 原始证据

实际生产执行结果统一放在 `docs/stage-new-4/experiments/`：

- HPA：`requests.jsonl`、`load-summary.json`、`hpa-timeline.tsv`、Pod/Node 资源、HPA YAML/describe、事件；
- 故障：故障前中后资源、全部 HTTP 响应及状态、独立服务健康结果、C 日志、事件、结构化 `summary.json`；
- 每个目录中的 `README.md` 只汇总原始文件中的实测数值，不以人工推断代替结果。

如果某项自动断言失败，脚本以非零状态退出，该轮不能作为课程通过证据；修正原因后必须重新完整执行。
