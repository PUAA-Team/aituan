# 视频 03：HPA 扩缩容完整命令与录制流程

> 目标：录到“加压前 1 个 Pod → 执行真实压力脚本 → CPU 上升 → HPA 增加 Pod → 压力结束 → Pod 自动回到 1”的全过程。正式实验使用 180 秒、40 并发，原始录屏通常需要约 5—9 分钟，最后剪成 60 秒。

如果录制电脑没有 kubeconfig，但可以 SSH 登录 k3s 服务器，请直接使用 `视频03-HPA扩缩容-SSH服务器录制版.md`。

## 1. 实验内容

生产 namespace 为 `aituan`。`api-gateway` 和 A/B/C/D 五个 Java 工作负载各有一个 HPA，CPU 目标 50%，副本范围 1—4。扩容无稳定等待，缩容有 120 秒稳定窗口。

压力脚本会在集群内创建临时 Node.js Job，通过 `api-gateway:8080` 等比例调用：

- A `identity-asset-service`：用户资料；
- B `merchant-catalog-service`：首页发现；
- C `trade-fulfillment-service`：订单列表；
- D `engagement-platform-service`：门店评价。

脚本每 10 秒记录 HPA 和 Pod 资源。压力结束后继续等待最多 360 秒，直到五个 HPA 都回到 1，并自动清理临时 Job、ConfigMap 和 Secret。

## 2. 录制准备

准备两个并排终端：终端 A 持续观察 HPA/Pod，终端 B 运行实验和展示结果。两个终端都进入仓库根目录：

```bash
cd /Users/camellia/Desktop/puaa/aituan-integration
```

录制期间不要同时运行性能测试、依赖故障实验或生产部署，并通知其他成员不要向同一集群施压。

## 3. 录制前预检

### 3.1 集群、服务与指标

在终端 B 执行：

```bash
kubectl config current-context
kubectl cluster-info
kubectl get nodes -o wide
kubectl -n aituan get deployments,pods,hpa -o wide
kubectl top nodes
kubectl -n aituan top pods
curl -fsS https://aituan.2b.gs/actuator/health | jq .
```

必须满足：节点为 `Ready`；Gateway、A/B/C/D 都 Ready；正好有 5 个 HPA；`TARGETS` 不是 `<unknown>`；五个 HPA 当前副本均为 1；公网健康为 `UP`。

下面的自动检查无输出且退出码为 0 才算通过：

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

若副本仍大于 1，说明上一轮还在冷却，应等待它自然回落。不要手工缩容来伪造基线。

### 3.2 遗留任务与权限

```bash
kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
kubectl auth can-i create jobs -n aituan
kubectl auth can-i create configmaps -n aituan
kubectl auth can-i create secrets -n aituan
kubectl auth can-i create pods/exec -n aituan
kubectl auth can-i get pods/log -n aituan
kubectl auth can-i delete jobs -n aituan
```

不应显示旧的 `aituan-hpa-load-*` 资源，权限检查均应输出 `yes`。如果发现资源，先确认没有其他成员正在实验。

## 4. 终端 A：开始录制并持续观察

先开始屏幕录制，再执行：

```bash
while true; do
  clear
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

先保留约 8 秒基线，确保画面看清 CPU 目标 50%、`MINPODS=1`、`MAXPODS=4`、当前副本 1。观察结束时按 `Ctrl+C`；该窗口只读，不会修改集群。

## 5. 终端 B：触发真实压力

执行：

```bash
export HPA_OUT="$PWD/docs/stage-new-4/recording-evidence/hpa-$(date +%Y%m%d-%H%M%S)"
echo "$HPA_OUT"

HPA_LOAD_DURATION_SECONDS=180 \
HPA_LOAD_CONCURRENCY=40 \
HPA_EXPERIMENT_OUTPUT_DIR="$HPA_OUT" \
bash scripts/experiments/run_hpa_experiment.sh
```

按下回车就是必须保留的“触发”动作。脚本运行期间不要启动第二份脚本，也不要关闭终端 B。

终端 A 应依次看到：

1. 出现 `aituan-hpa-load-*` 压力 Job；
2. HPA 的 `TARGETS` CPU 上升；
3. CPU 超过 50% 后，`REPLICAS` 从 1 增加到 2—4；
4. Pod 列表出现同一服务的新 Pod 并变为 Ready；
5. 压力结束后，副本经过稳定窗口逐步回到 1。

HPA 扩容必须由 CPU 指标自动触发。严禁执行 `kubectl scale`，也不要修改 HPA 的最小、最大副本制造画面。

可选：第三个终端查看压力日志：

```bash
kubectl -n aituan logs \
  -l app.kubernetes.io/name=aituan-hpa-load \
  --tail=20 -f
```

日志会先显示 `load-start`，180 秒后显示 `load-finish` 和聚合结果。

## 6. 判断是否成功

终端 B 最后必须出现：

```text
HPA experiment passed; evidence: <本轮证据目录>
```

这表示脚本已经验证：产生真实请求；总错误率不超过 5%；A/B/C/D 中至少两个服务曾超过 1 个副本；五个 HPA 最终都回到 1；临时资源已进入自动清理。

如果脚本非零退出，本轮不能作为成功证据，必须排查后完整重录。

## 7. 展示最终结果

先显示请求、延迟和错误率：

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
    averageMs: .overall.averageMs,
    p95Ms: .overall.p95Ms,
    p99Ms: .overall.p99Ms
  }
}' "$HPA_OUT/load-summary.json"
```

再显示每个服务达到的最大副本数：

```bash
awk -F '\t' '
  NR > 1 && $4 + 0 > max[$3] { max[$3] = $4 + 0 }
  END { for (service in max) print service, "maxReplicas=" max[service] }
' "$HPA_OUT/hpa-timeline.tsv" | sort
```

最后证明自动回落：

```bash
kubectl -n aituan get hpa
kubectl -n aituan get pods -o wide
kubectl -n aituan get hpa -o json | jq -e '
  all(.items[];
    (.status.currentReplicas // 0) == 1 and
    (.status.desiredReplicas // 0) == 1
  )
'
```

最终画面至少要看清：请求数大于 0、错误率不超过 5%；至少两个业务服务的 `maxReplicas` 大于 1；最终五个 HPA 均为 `1/1`；Pod 没有持续 `CrashLoopBackOff` 或大量重启。

## 8. 剪成 60 秒

| 成片时间 | 使用的原始画面 | 字幕 / 配音 |
| ---: | --- | --- |
| 0:00—0:08 | 5 个 HPA、每个 1 Pod、CPU 目标 50% | “加压前，五个 Java 工作负载各一个 Pod，CPU 目标为 50%。” |
| 0:08—0:15 | 输入正式命令，压力 Job 出现 | “现在通过 Gateway 向四个真实业务服务持续加压。” |
| 0:15—0:32 | 扩容过程 ×6—8，保留 CPU 超标、`1→2→4` 和新 Pod Ready | “CPU 超过目标后，HPA 自动创建新 Pod。” |
| 0:32—0:50 | 压力结束和冷却过程 ×6—8，显示 `4→2→1` | “压力下降后，经过 120 秒稳定窗口，副本自动回落。” |
| 0:50—1:00 | 聚合结果、最大副本和最终 `1/1` | “脚本校验错误率、扩容结果和最终恢复，本轮实验通过。” |

所有加速片段叠字：`原始过程 ×8，同一实验目录，时间戳连续`。不能用另一轮的扩容和本轮的缩容拼接。

## 9. 中断、失败和清理

### 9.1 正常完成或按 `Ctrl+C`

```bash
kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
kubectl -n aituan get hpa
kubectl -n aituan get pods
```

正常完成后不应留下临时资源。按 `Ctrl+C` 时脚本的 `trap` 会尝试清理；压力消失后仍应等待最多 6 分钟自然回落，不要手工缩容。

### 9.2 HPA 不扩容或显示 `<unknown>`

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

常见原因：metrics-server 无指标、压力镜像拉取失败、演示账号登录失败、集群资源不足或其他实验正在占用集群。排除后从基线完整重录，不要在失败的一半更换参数。

### 9.3 强制关闭后仍有临时资源

先取得准确名称：

```bash
kubectl -n aituan get jobs,configmaps,secrets | grep aituan-hpa-load || true
```

确认确属本轮实验且没有其他成员使用后，再逐个删除准确名称：

```bash
kubectl -n aituan delete job <准确的Job名称>
kubectl -n aituan delete configmap <准确的ConfigMap名称>
kubectl -n aituan delete secret <准确的Secret名称>
```

不要用通配符批量删除 namespace 中的资源。

## 10. 证据文件

成功后证据位于 `$HPA_OUT`：

```text
README.md
load-summary.json
requests.jsonl
hpa-timeline.tsv
pod-resource-timeline.txt
hpa-before.yaml
hpa-after.yaml
hpa-describe-after-load.txt
events-after-load.txt
workloads-before.txt
workloads-after.txt
```

归档或提交前应检查文件不含访问令牌、密码或个人数据；体积较大的 `requests.jsonl` 不需要放入 PPT，也不要未经检查直接提交仓库。
