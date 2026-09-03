# 性能对比证据说明

本目录保存提交 `65ddbf8e2fa0b4149552c4223d68f60a1ef9d468` 在 `aituan-new` 上完成的单体/微服务同条件三轮性能汇总、机器配置、资源范围和响应等价性证据。

逐请求 JSONL 和每 500 ms 的进程资源 JSONL 体积较大，统一压缩在：

`tests/performance/results/aituan-performance-comparison-20260902-server-final.tar.gz`

SHA-256：`84d53a05b0b51c0eb7d81f5a4579df15f8ee5c51375908ea31f775a995dc8f81`

完整方法、结论、原因和局限见：

`docs/stage-new-4/单体与微服务同条件三轮性能对比报告.md`
