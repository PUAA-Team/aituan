# 单体与微服务统一性能测试

本目录提供不依赖第三方压测工具的统一脚本。单体和微服务 Gateway 必须使用同一台机器、同一份 seed、同一个场景文件和完全相同的参数，目标按顺序测试，避免同时运行互相争抢资源。

## 默认口径

- 场景：主页、推荐列表、门店搜索三个主要只读接口。
- 数据：`database/microservices/merchant/seeds/R__seed_merchant_catalog_demo.sql`；单体测试时复制到隔离 schema，不能直接使用数据量更大的旧单体 seed。
- 并发：50。
- 每轮：15000 个请求，另有 1000 个预热请求。
- 重复：每个版本至少 3 轮。
- 指标：吞吐量、平均/P95/P99、错误率；指定服务 PID 后同时记录 CPU 和常驻内存。
- 原始数据：每个请求和每次资源采样各保存一行 JSONL，同时生成 JSON/CSV 汇总及机器配置。
- 稳定性：可通过 `--cooldown-ms` 设置轮间冷却；Linux/K3s 可通过 `--proc-root` 从只读宿主 `/proc` 计算进程 CPU 时间增量。

## 运行示例

只测单体：

```powershell
node tests/performance/load-test.mjs `
  --target monolith=http://127.0.0.1:18080 `
  --pids monolith=12345 `
  --runs 3 `
  --concurrency 50 `
  --requests 15000 `
  --output tests/performance/results/monolith-20260901
```

微服务合并并通过 Gateway 启动后，使用同一个命令同时传入两个目标：

```powershell
node tests/performance/load-test.mjs `
  --target monolith=http://127.0.0.1:18080 `
  --target microservices=http://127.0.0.1:28080 `
  --pids monolith=12345 `
  --pids microservices=22341,22342,22343,22344,22345 `
  --runs 3 `
  --concurrency 50 `
  --requests 15000 `
  --cooldown-ms 10000 `
  --output tests/performance/results/comparison-20260901
```

`--pids` 可省略，但 CPU/内存会标记为不可用。课程最终口径中，单体 PID 应包含单体 Java 与 MySQL，微服务 PID 应包含 Gateway、A/B/C/D 与 MySQL，才能同时反映热链路和整套架构资源代价。脚本不会自动启动或停止服务，防止把构建、启动时间计入业务性能。

K3s 隔离单体和集群内压测器清单位于 `tests/performance/k8s/`。压测器可只读挂载宿主 `/proc`，通过 `--proc-root /host-proc` 采集 K3s 容器进程；正式测试时应临时固定副本数，结束后恢复 HPA。

## 输出结构

```text
results/<test-id>/
├─ machine.json
├─ test-config.json
├─ summary.json
├─ summary.csv
├─ raw/<target>-run-01.jsonl
└─ resources/<target>-run-01.jsonl
```

只有单体和微服务均完成至少三轮、错误率可接受，且机器、数据、业务响应和副本条件一致时，才能据此写“性能提升”。提交 `65ddbf8` 的正式结果已归档为 `tests/performance/results/aituan-performance-comparison-20260902-server-final.tar.gz`，结论见 `docs/stage-new-4/单体与微服务同条件三轮性能对比报告.md`；旧 `monolith-20260901-final.zip` 来自 Apple M5 + H2，不得与服务器微服务结果直接比较。
