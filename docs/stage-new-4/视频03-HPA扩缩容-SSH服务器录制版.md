# 视频 03：HPA 扩缩容——SSH 服务器录制版

> 适用情况：录制电脑已经安装 `kubectl`，但没有 kubeconfig；可以 SSH 登录运行 k3s 的服务器。以下方案不修改生产部署目录，也不要求本机直接连接 Kubernetes API。

## 0. 当前服务器准备状态（2026-09-03）

两个实验文件已经上传到当前生产 SSH 别名 `aituan-new`：

```text
/tmp/aituan-hpa-demo/run_hpa_experiment.sh
/tmp/aituan-hpa-demo/hpa-load.mjs
```

本地与远端 SHA-256 已逐一比对一致，远端 Bash 语法检查通过。服务器已有 `/usr/local/bin/kubectl`、`k3s`、`jq` 和 `awk`，当前 5 个 HPA 均有 CPU 指标且副本为 1。因此本次录制可以直接执行 `ssh aituan-new`，不再需要重复第 2 节的上传步骤，也不需要配置本机 kubeconfig。

## 1. 前提条件

SSH 账号必须能够执行：

```bash
sudo k3s kubectl get nodes
```

服务器还需要有 `bash`、`jq` 和 `awk`。先登录服务器，在正式录制前检查：

```bash
sudo -v
sudo k3s kubectl get nodes
command -v bash
command -v jq
command -v awk
```

若 `sudo k3s kubectl get nodes` 没有权限，不要临时使用来历不明的管理员凭据，应让集群负责人授予本次演示所需权限。

## 2. 录制前：从本机上传实验文件

这一部分不用录。先在本机仓库根目录执行，把占位符替换为实际 SSH 信息：

```bash
cd /Users/camellia/Desktop/puaa/aituan-integration

ssh -p <SSH端口> <SSH用户>@<服务器地址> \
  'mkdir -p /tmp/aituan-hpa-demo'

scp -P <SSH端口> \
  scripts/experiments/run_hpa_experiment.sh \
  scripts/experiments/hpa-load.mjs \
  <SSH用户>@<服务器地址>:/tmp/aituan-hpa-demo/
```

上传后校验文件存在：

```bash
ssh -p <SSH端口> <SSH用户>@<服务器地址> \
  'ls -l /tmp/aituan-hpa-demo/run_hpa_experiment.sh /tmp/aituan-hpa-demo/hpa-load.mjs'
```

不要上传 kubeconfig、SSH 私钥、`.env` 或 GitHub Secrets。压力账号密码由实验脚本使用默认演示账号，并通过临时 Kubernetes Secret 传递，不会写入上传的两个文件。

## 3. 打开两个 SSH 终端

当前项目打开两个本地终端窗口，都执行：

```bash
ssh aituan-new
```

当前服务器已经提供 `/usr/local/bin/kubectl`，登录后直接验证：

```bash
kubectl get nodes
kubectl -n aituan get hpa
```

若以后更换为非 root SSH 账号且服务器没有独立 `kubectl`，才需要先进入 Bash，再导出包装函数：

```bash
exec bash
sudo -v
kubectl() { sudo k3s kubectl "$@"; }
export -f kubectl
```

不能只写 `alias kubectl='sudo k3s kubectl'`，因为实验脚本启动的子 Bash 不会继承普通 alias。

## 4. 终端 B：正式预检

在终端 B 执行：

```bash
cd /tmp/aituan-hpa-demo

kubectl get nodes -o wide
kubectl -n aituan get deployments,pods,hpa -o wide
kubectl top nodes
kubectl -n aituan top pods
kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
```

必须看到：

- 节点为 `Ready`；
- Gateway、A/B/C/D 均 Ready；
- 正好有 5 个 HPA；
- `TARGETS` 不是 `<unknown>`；
- 五个 HPA 当前副本均为 1；
- 没有其他成员正在运行的 `aituan-hpa-load-*`。

执行自动基线断言：

```bash
kubectl -n aituan get hpa -o json | jq -e '
  (.items | length) == 5 and
  all(.items[];
    (.status.currentReplicas // 0) == 1 and
    (.status.desiredReplicas // 0) == 1 and
    ((.status.currentMetrics // []) | length) > 0 and
    .spec.minReplicas == 1 and
    .spec.maxReplicas == 4
  )
'
```

输出 `true` 才开始录制。副本大于 1 时等待自然回落，不要执行手工缩容。

## 5. 终端 A：开始录制并观察

先开始屏幕录制，再在终端 A 执行：

```bash
while true; do
  clear
  hostname
  date
  echo '=== HPA ==='
  kubectl -n aituan get hpa
  echo '=== PODS ==='
  kubectl -n aituan get pods -o wide
  echo '=== LOAD JOB ==='
  kubectl -n aituan get jobs -l app.kubernetes.io/name=aituan-hpa-load
  sleep 5
done
```

先录约 8 秒基线，保留服务器 hostname、5 个 HPA、CPU 目标 50% 和当前 1 个副本。hostname 只用于证明当前操作发生在服务器终端；若它包含敏感信息，剪辑时打码。

## 6. 终端 B：从服务器触发实验

在终端 B 执行：

```bash
cd /tmp/aituan-hpa-demo

export HPA_OUT="/tmp/aituan-hpa-demo/evidence/hpa-$(date +%Y%m%d-%H%M%S)"
echo "$HPA_OUT"

HPA_LOAD_DURATION_SECONDS=180 \
HPA_LOAD_CONCURRENCY=40 \
HPA_EXPERIMENT_OUTPUT_DIR="$HPA_OUT" \
bash ./run_hpa_experiment.sh
```

按下回车就是视频中必须保留的触发动作。脚本会调用已经导出的 `kubectl` 函数，实际执行的是 `sudo k3s kubectl`。

终端 A 应依次看到：

1. `aituan-hpa-load-*` Job 出现；
2. `TARGETS` CPU 上升并超过 50%；
3. HPA 副本从 `1→2→4`，或至少从 1 增加到 2；
4. 新 Pod 创建并变为 Ready；
5. 180 秒压力结束后，副本经过稳定窗口逐渐回到 1。

整个过程中不能使用 `kubectl scale`，也不能修改 HPA 的 `minReplicas` 或 `maxReplicas`。

## 7. 实验结束后展示结果

终端 B 最后必须出现：

```text
HPA experiment passed; evidence: /tmp/aituan-hpa-demo/evidence/...
```

随后执行：

```bash
jq '{
  startedAt,
  endedAt,
  config: {
    durationSeconds: .config.durationSeconds,
    concurrency: .config.concurrency,
    targetCount: .config.targetCount
  },
  result: {
    requests: .overall.requests,
    successes: .overall.successes,
    errors: .overall.errors,
    errorRate: .overall.errorRate,
    throughputRps: .overall.throughputRps,
    p95Ms: .overall.p95Ms,
    p99Ms: .overall.p99Ms
  }
}' "$HPA_OUT/load-summary.json"

awk -F '\t' '
  NR > 1 && $4 + 0 > max[$3] { max[$3] = $4 + 0 }
  END { for (service in max) print service, "maxReplicas=" max[service] }
' "$HPA_OUT/hpa-timeline.tsv" | sort

kubectl -n aituan get hpa
kubectl -n aituan get pods -o wide
```

画面应同时证明：请求数大于 0、错误率不超过 5%、至少两个业务服务曾扩容、最终五个 HPA 都回到 1、Pod 没有持续异常或大量重启。

终端 A 观察结束后按 `Ctrl+C`。

## 8. 剪成 60 秒

| 成片时间 | 画面 |
| ---: | --- |
| 0:00—0:08 | SSH 终端 A：hostname、5 个 HPA、每个 1 Pod、CPU 目标 50% |
| 0:08—0:15 | SSH 终端 B：执行 180 秒、40 并发命令；终端 A 出现 Job |
| 0:15—0:32 | 扩容过程加速 6—8 倍，显示 CPU 上升、`1→2→4` 和新 Pod Ready |
| 0:32—0:50 | 压力结束与冷却过程加速 6—8 倍，显示 `4→2→1` |
| 0:50—1:00 | 请求/错误率、最大副本统计、最终五个 HPA 为 1 |

加速画面叠字：`服务器原始录屏 ×8，同一实验目录，时间戳连续`。不要用不同轮次拼接扩容和回落。

## 9. 将证据下载回本机

记下终端 B 打印的准确 `$HPA_OUT` 路径。退出 SSH 后在本机执行，把示例路径换成本轮实际路径：

```bash
cd /Users/camellia/Desktop/puaa/aituan-integration

scp -P <SSH端口> -r \
  <SSH用户>@<服务器地址>:/tmp/aituan-hpa-demo/evidence/hpa-<实际时间戳> \
  docs/stage-new-4/recording-evidence/
```

下载后检查 `README.md`、`load-summary.json` 和 `hpa-timeline.tsv`。`requests.jsonl` 体积可能较大，不需要放入 PPT，也不要未经敏感信息检查直接提交仓库。

## 10. 中断或失败

脚本正常结束或按 `Ctrl+C` 时会自动清理临时 Job、ConfigMap、Secret。检查：

```bash
kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
kubectl -n aituan get hpa
kubectl -n aituan get pods
```

如果 HPA 没有扩容或显示 `<unknown>`：

```bash
kubectl top nodes
kubectl -n aituan top pods
kubectl -n aituan get jobs,pods
kubectl -n aituan logs \
  -l app.kubernetes.io/name=aituan-hpa-load \
  --tail=100
kubectl -n aituan describe hpa
kubectl -n aituan get events --sort-by=.lastTimestamp
```

若 SSH 连接被强制断开，先精确列出遗留资源并确认它们属于本轮实验：

```bash
sudo k3s kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
```

再由集群负责人逐个删除准确名称。不要使用通配符批量删除，也不要手工把业务 Deployment 缩回 1；删除压力源后应等待 HPA 自然回落。
