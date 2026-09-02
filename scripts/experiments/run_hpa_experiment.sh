#!/usr/bin/env bash
set -euo pipefail

namespace="${K8S_NAMESPACE:-aituan}"
duration_seconds="${HPA_LOAD_DURATION_SECONDS:-180}"
concurrency="${HPA_LOAD_CONCURRENCY:-40}"
account="${HPA_LOAD_ACCOUNT:-demo_user}"
password="${HPA_LOAD_PASSWORD:-123456}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
output_dir="${HPA_EXPERIMENT_OUTPUT_DIR:-${PWD}/docs/stage-new-4/experiments/hpa-${timestamp}}"
suffix="$(date +%s)"
configmap="aituan-hpa-load-${suffix}"
secret="aituan-hpa-load-${suffix}"
job="aituan-hpa-load-${suffix}"
timeline="${output_dir}/hpa-timeline.tsv"
top_samples="${output_dir}/pod-resource-timeline.txt"

mkdir -p "${output_dir}"

cleanup() {
  kubectl -n "${namespace}" delete job "${job}" configmap "${configmap}" secret "${secret}" --ignore-not-found --wait=false >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

for command in kubectl jq awk; do
  command -v "${command}" >/dev/null || { echo "missing required command: ${command}" >&2; exit 1; }
done

hpa_count="$(kubectl -n "${namespace}" get hpa -o json | jq '.items | length')"
test "${hpa_count}" = 5 || { echo "expected 5 HPAs, found ${hpa_count}" >&2; exit 1; }
for service in api-gateway identity-asset-service merchant-catalog-service trade-fulfillment-service engagement-platform-service; do
  test "$(kubectl -n "${namespace}" get "hpa/${service}" -o jsonpath='{.spec.scaleTargetRef.name}')" = "${service}"
  kubectl -n "${namespace}" rollout status "deployment/${service}" --timeout=300s
done

kubectl version -o yaml > "${output_dir}/kubernetes-version.yaml"
kubectl get nodes -o wide > "${output_dir}/nodes.txt"
kubectl top nodes > "${output_dir}/node-resources-before.txt"
kubectl -n "${namespace}" get hpa -o yaml > "${output_dir}/hpa-before.yaml"
kubectl -n "${namespace}" get deployments,pods -o wide > "${output_dir}/workloads-before.txt"

printf 'timestamp\tphase\thpa\tcurrentReplicas\tdesiredReplicas\tcurrentCpu\ttargetCpu\n' > "${timeline}"
: > "${top_samples}"

record_sample() {
  local phase="$1" now
  now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  kubectl -n "${namespace}" get hpa -o json | jq -r --arg now "${now}" --arg phase "${phase}" '
    .items[] |
    [$now, $phase, .metadata.name, (.status.currentReplicas // 0), (.status.desiredReplicas // 0),
     (.status.currentMetrics[0].resource.current.averageUtilization // "unknown"),
     (.spec.metrics[0].resource.target.averageUtilization // "unknown")] | @tsv
  ' >> "${timeline}"
  {
    printf '\n[%s] phase=%s\n' "${now}" "${phase}"
    kubectl -n "${namespace}" top pods --containers 2>&1 || true
  } >> "${top_samples}"
}

record_sample baseline
kubectl -n "${namespace}" create configmap "${configmap}" --from-file=hpa-load.mjs="${script_dir}/hpa-load.mjs" >/dev/null
kubectl -n "${namespace}" create secret generic "${secret}" --from-literal=account="${account}" --from-literal=password="${password}" >/dev/null

kubectl -n "${namespace}" apply -f - >/dev/null <<YAML
apiVersion: batch/v1
kind: Job
metadata:
  name: ${job}
  labels: {app.kubernetes.io/name: aituan-hpa-load, app.kubernetes.io/part-of: aituan}
spec:
  backoffLimit: 0
  ttlSecondsAfterFinished: 900
  template:
    metadata:
      labels: {app.kubernetes.io/name: aituan-hpa-load, app.kubernetes.io/part-of: aituan}
    spec:
      restartPolicy: Never
      containers:
        - name: load
          image: node:24-alpine
          command: [node, /scripts/hpa-load.mjs]
          env:
            - {name: LOAD_ORIGIN, value: "http://api-gateway:8080"}
            - {name: LOAD_DURATION_SECONDS, value: "${duration_seconds}"}
            - {name: LOAD_CONCURRENCY, value: "${concurrency}"}
            - name: LOAD_ACCOUNT
              valueFrom: {secretKeyRef: {name: ${secret}, key: account}}
            - name: LOAD_PASSWORD
              valueFrom: {secretKeyRef: {name: ${secret}, key: password}}
          resources:
            requests: {cpu: 100m, memory: 96Mi}
            limits: {cpu: "2", memory: 512Mi}
          volumeMounts:
            - {name: script, mountPath: /scripts, readOnly: true}
            - {name: results, mountPath: /results}
      volumes:
        - name: script
          configMap: {name: ${configmap}}
        - name: results
          emptyDir: {}
YAML

deadline="$(( $(date +%s) + duration_seconds + 300 ))"
while true; do
  record_sample load
  succeeded="$(kubectl -n "${namespace}" get "job/${job}" -o jsonpath='{.status.succeeded}')"
  failed="$(kubectl -n "${namespace}" get "job/${job}" -o jsonpath='{.status.failed}')"
  [[ "${succeeded:-0}" -ge 1 ]] && break
  if [[ "${failed:-0}" -ge 1 ]]; then
    kubectl -n "${namespace}" logs "job/${job}" > "${output_dir}/load-job.log" 2>&1 || true
    echo "load job failed" >&2
    exit 1
  fi
  [[ "$(date +%s)" -lt "${deadline}" ]] || { echo "load job timed out" >&2; exit 1; }
  sleep 10
done

record_sample load-finished
pod="$(kubectl -n "${namespace}" get pods -l "job-name=${job}" -o jsonpath='{.items[0].metadata.name}')"
kubectl -n "${namespace}" logs "${pod}" > "${output_dir}/load-job.log"
kubectl -n "${namespace}" cp "${pod}:/results/." "${output_dir}" >/dev/null
kubectl -n "${namespace}" describe hpa > "${output_dir}/hpa-describe-after-load.txt"
kubectl -n "${namespace}" get events --sort-by=.lastTimestamp > "${output_dir}/events-after-load.txt"

scaled_business_services="$(awk -F '\t' 'NR > 1 && $3 != "api-gateway" && ($4 + 0) > 1 {seen[$3]=1} END {print length(seen)}' "${timeline}")"
if [[ "${scaled_business_services}" -lt 2 ]]; then
  echo "only ${scaled_business_services} business service(s) reached more than one replica" >&2
  exit 1
fi

scale_down_deadline="$(( $(date +%s) + 360 ))"
while true; do
  record_sample cooldown
  replicas_above_min="$(kubectl -n "${namespace}" get hpa -o json | jq '[.items[] | select((.status.currentReplicas // 0) != 1 or (.status.desiredReplicas // 0) != 1)] | length')"
  [[ "${replicas_above_min}" = 0 ]] && break
  [[ "$(date +%s)" -lt "${scale_down_deadline}" ]] || { echo "HPAs did not return to one replica in time" >&2; exit 1; }
  sleep 10
done

record_sample recovered
kubectl -n "${namespace}" get hpa -o yaml > "${output_dir}/hpa-after.yaml"
kubectl -n "${namespace}" get deployments,pods -o wide > "${output_dir}/workloads-after.txt"
kubectl top nodes > "${output_dir}/node-resources-after.txt"

jq -e '.overall.requests > 0 and .overall.errorRate <= 0.05' "${output_dir}/load-summary.json" >/dev/null
{
  echo '# HPA 实压扩缩容实验摘要'
  echo
  echo "- UTC 开始时间：$(jq -r '.startedAt' "${output_dir}/load-summary.json")"
  echo "- 持续时间：${duration_seconds} 秒"
  echo "- 并发：${concurrency}"
  echo "- 总请求数：$(jq -r '.overall.requests' "${output_dir}/load-summary.json")"
  echo "- 吞吐：$(jq -r '.overall.throughputRps' "${output_dir}/load-summary.json") req/s"
  echo "- 平均/P95/P99：$(jq -r '.overall.averageMs' "${output_dir}/load-summary.json") / $(jq -r '.overall.p95Ms' "${output_dir}/load-summary.json") / $(jq -r '.overall.p99Ms' "${output_dir}/load-summary.json") ms"
  echo "- 错误率：$(jq -r '.overall.errorRate' "${output_dir}/load-summary.json")"
  echo "- 实际扩容到 2 个以上 Pod 的业务微服务数：${scaled_business_services}/4"
  echo '- 冷却后结果：5 个 HPA 均恢复为 1 个 Pod。'
  echo '- 原始文件：requests.jsonl、hpa-timeline.tsv、pod-resource-timeline.txt、HPA YAML/describe、Kubernetes/节点配置和事件。'
} > "${output_dir}/README.md"

echo "HPA experiment passed; evidence: ${output_dir}"
