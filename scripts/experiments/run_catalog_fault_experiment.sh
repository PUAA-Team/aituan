#!/usr/bin/env bash
set -euo pipefail

namespace="${K8S_NAMESPACE:-aituan}"
origin="${FAULT_TEST_ORIGIN:-https://aituan.2b.gs}"
account="${FAULT_TEST_ACCOUNT:-demo_user}"
password="${FAULT_TEST_PASSWORD:-123456}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output_dir="${FAULT_EXPERIMENT_OUTPUT_DIR:-${PWD}/docs/stage-new-4/experiments/catalog-fault-${timestamp}}"
catalog_service="merchant-catalog-service"
hpa_target=""
fault_injected=false
fault_started_epoch=0
mkdir -p "${output_dir}"

cleanup() {
  if [[ "${fault_injected}" = true ]]; then
    kubectl -n "${namespace}" patch "hpa/${catalog_service}" --type=merge -p "{\"spec\":{\"scaleTargetRef\":{\"name\":\"${hpa_target}\"}}}" >/dev/null 2>&1 || true
    kubectl -n "${namespace}" scale "deployment/${catalog_service}" --replicas=1 >/dev/null 2>&1 || true
    kubectl -n "${namespace}" rollout status "deployment/${catalog_service}" --timeout=300s >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

for command in kubectl jq curl; do
  command -v "${command}" >/dev/null || { echo "missing required command: ${command}" >&2; exit 1; }
done

request() {
  local name="$1" method="$2" path="$3" body="${4:-}"
  local request_id="fault-${timestamp}-${name}"
  local -a arguments=(-ksS --max-time 12 -X "${method}" -H "Authorization: Bearer ${token}" -H "X-Request-Id: ${request_id}" -o "${output_dir}/${name}.json" -w '%{http_code}')
  if [[ -n "${body}" ]]; then
    arguments+=(-H 'Content-Type: application/json' --data "${body}")
  fi
  curl "${arguments[@]}" "${origin}${path}" > "${output_dir}/${name}.http-status"
}

require_api_code() {
  local name="$1" expected="$2"
  actual="$(jq -r '.code // "missing"' "${output_dir}/${name}.json")"
  test "${actual}" = "${expected}" || { echo "${name}: expected API code ${expected}, got ${actual}" >&2; exit 1; }
}

login_response="$(curl -ksS --max-time 12 -H 'Content-Type: application/json' --data "{\"account\":\"${account}\",\"password\":\"${password}\"}" "${origin}/api/open/auth/user/login/password")"
token="$(jq -r '.data.accessToken // .data.token // empty' <<<"${login_response}")"
test -n "${token}" || { echo "fault experiment login failed" >&2; exit 1; }

for service in api-gateway identity-asset-service merchant-catalog-service trade-fulfillment-service engagement-platform-service; do
  kubectl -n "${namespace}" rollout status "deployment/${service}" --timeout=300s
done
hpa_target="$(kubectl -n "${namespace}" get "hpa/${catalog_service}" -o jsonpath='{.spec.scaleTargetRef.name}')"
test "${hpa_target}" = "${catalog_service}"

kubectl -n "${namespace}" get deployments,pods,hpa -o wide > "${output_dir}/resources-before.txt"
request baseline-clear DELETE '/api/app/trade/cart?storeId=1'
require_api_code baseline-clear 0
request baseline-add POST '/api/app/trade/cart/items' '{"storeId":1,"itemId":1002,"quantity":2}'
require_api_code baseline-add 0
jq -e '.data.catalogAvailable == true and (.data.items | length) == 1 and .data.items[0].itemName == "吮指原味鸡"' "${output_dir}/baseline-add.json" >/dev/null
request baseline-cart GET '/api/app/trade/cart?storeId=1'
require_api_code baseline-cart 0

kubectl -n "${namespace}" patch "hpa/${catalog_service}" --type=merge -p '{"spec":{"scaleTargetRef":{"name":"merchant-catalog-service-fault-injection"}}}' >/dev/null
fault_injected=true
fault_started_epoch="$(date +%s)"
kubectl -n "${namespace}" scale "deployment/${catalog_service}" --replicas=0 >/dev/null
for attempt in $(seq 1 30); do
  replicas="$(kubectl -n "${namespace}" get "deployment/${catalog_service}" -o jsonpath='{.status.replicas}')"
  [[ "${replicas:-0}" = 0 ]] && break
  [[ "${attempt}" != 30 ]] || { echo "catalog deployment did not stop" >&2; exit 1; }
  sleep 2
done
kubectl -n "${namespace}" get deployments,pods,hpa -o wide > "${output_dir}/resources-during-fault.txt"

catalog_http="$(curl -ksS --max-time 12 -o "${output_dir}/catalog-during-fault.json" -w '%{http_code}' "${origin}/api/app/discovery/home" || true)"
printf '%s' "${catalog_http}" > "${output_dir}/catalog-during-fault.http-status"
if [[ "${catalog_http}" = 200 ]] && jq -e '.code == 0' "${output_dir}/catalog-during-fault.json" >/dev/null 2>&1; then
  echo "catalog request unexpectedly succeeded during fault" >&2
  exit 1
fi

request degraded-cart GET '/api/app/trade/cart?storeId=1'
require_api_code degraded-cart 0
jq -e '.data.catalogAvailable == false and (.data.notice | contains("商品服务暂不可用")) and (.data.items | length) == 1 and .data.items[0].itemName == "吮指原味鸡" and .data.items[0].quantity == 2' "${output_dir}/degraded-cart.json" >/dev/null

request rejected-add POST '/api/app/trade/cart/items' '{"storeId":1,"itemId":1001,"quantity":1}'
require_api_code rejected-add 9999
jq -e '.message | contains("商品服务暂不可用")' "${output_dir}/rejected-add.json" >/dev/null
request unchanged-cart GET '/api/app/trade/cart?storeId=1'
require_api_code unchanged-cart 0
jq -e '(.data.items | length) == 1 and .data.items[0].itemId == 1002 and .data.items[0].quantity == 2' "${output_dir}/unchanged-cart.json" >/dev/null

request identity-still-works GET '/api/app/account/profile'
require_api_code identity-still-works 0
request trade-still-works GET '/api/app/trade/orders?page=1&pageSize=20'
require_api_code trade-still-works 0
request engagement-still-works GET '/api/app/interaction/stores/1/reviews?page=1&pageSize=20'
require_api_code engagement-still-works 0

health_pod="fault-health-${timestamp,,}"
health_pod="${health_pod:0:62}"
kubectl -n "${namespace}" run "${health_pod}" --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- \
  sh -ec 'for endpoint in api-gateway:8080 identity-asset-service:8081 trade-fulfillment-service:8083 engagement-platform-service:8084; do printf "%s " "$endpoint"; curl -fsS --max-time 5 "http://$endpoint/actuator/health"; echo; done' \
  > "${output_dir}/independent-health-during-fault.txt"

request degraded-remove DELETE '/api/app/trade/cart/items/1002?storeId=1'
require_api_code degraded-remove 0
jq -e '.data.catalogAvailable == false and (.data.items | length) == 0' "${output_dir}/degraded-remove.json" >/dev/null

kubectl -n "${namespace}" patch "hpa/${catalog_service}" --type=merge -p "{\"spec\":{\"scaleTargetRef\":{\"name\":\"${hpa_target}\"}}}" >/dev/null
kubectl -n "${namespace}" scale "deployment/${catalog_service}" --replicas=1 >/dev/null
kubectl -n "${namespace}" rollout status "deployment/${catalog_service}" --timeout=300s
fault_injected=false

for attempt in $(seq 1 30); do
  recovery_http="$(curl -ksS --max-time 10 -o "${output_dir}/catalog-recovery-probe.json" -w '%{http_code}' "${origin}/api/app/discovery/home" || true)"
  if [[ "${recovery_http}" = 200 ]] && jq -e '.code == 0' "${output_dir}/catalog-recovery-probe.json" >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" != 30 ]] || { echo "catalog service did not recover" >&2; exit 1; }
  sleep 2
done
recovery_seconds="$(( $(date +%s) - fault_started_epoch ))"

request recovered-add POST '/api/app/trade/cart/items' '{"storeId":1,"itemId":1001,"quantity":1}'
require_api_code recovered-add 0
jq -e '.data.catalogAvailable == true and (.data.items | length) == 1 and .data.items[0].itemName == "香辣鸡腿堡"' "${output_dir}/recovered-add.json" >/dev/null
request recovered-clear DELETE '/api/app/trade/cart?storeId=1'
require_api_code recovered-clear 0

health_pod="recovery-health-${timestamp,,}"
health_pod="${health_pod:0:62}"
kubectl -n "${namespace}" run "${health_pod}" --rm -i --restart=Never --image=curlimages/curl:8.10.1 -- \
  sh -ec 'for endpoint in api-gateway:8080 identity-asset-service:8081 merchant-catalog-service:8082 trade-fulfillment-service:8083 engagement-platform-service:8084; do printf "%s " "$endpoint"; curl -fsS --max-time 5 "http://$endpoint/actuator/health"; echo; done' \
  > "${output_dir}/all-health-after-recovery.txt"

kubectl -n "${namespace}" get deployments,pods,hpa -o wide > "${output_dir}/resources-after-recovery.txt"
while IFS= read -r trade_pod; do
  echo "===== ${trade_pod} ====="
  kubectl -n "${namespace}" logs "${trade_pod}" --since=15m 2>&1 || true
done < <(kubectl -n "${namespace}" get pods -l app.kubernetes.io/name=trade-fulfillment-service -o name) > "${output_dir}/trade-logs.txt"
kubectl -n "${namespace}" get events --sort-by=.lastTimestamp > "${output_dir}/events.txt"

jq -n \
  --arg timestamp "${timestamp}" \
  --arg origin "${origin}" \
  --argjson recoverySeconds "${recovery_seconds}" \
  '{experiment:"merchant-catalog dependency failure and recovery", timestamp:$timestamp, origin:$origin, recoverySeconds:$recoverySeconds, assertions:{catalogUnavailable:true, cachedCartReadable:true, writesFailFastWithoutPartialMutation:true, removeStillAvailable:true, identityHealthy:true, tradeHealthy:true, engagementHealthy:true, catalogRecovered:true, cartReturnedToNormal:true}}' \
  > "${output_dir}/summary.json"

{
  echo '# 商品服务依赖故障、隔离与恢复实验摘要'
  echo
  echo "- UTC 实验时间：${timestamp}"
  echo '- 故障注入：暂停商品服务 HPA 目标并将 B Deployment 缩容到 0。'
  echo '- 降级行为：C 从自有数据库的购物车快照返回门店、商品、价格和数量，并明确标识 catalogAvailable=false。'
  echo '- 数据安全：新增/改数量快速返回 9999，购物车不产生部分写入；移除/清空仍可使用。'
  echo '- 故障隔离：Gateway、A、C、D 的健康端点均为 UP，A 用户资料、C 订单列表、D 评价列表均成功。'
  echo "- 恢复：B rollout 完成后 ${recovery_seconds} 秒内公开商品接口和购物车写操作恢复，catalogAvailable=true。"
  echo '- 清理：实验购物车已清空，HPA 已重新绑定原 Deployment。'
  echo '- 原始文件：所有 HTTP 响应/状态、故障前中后资源、服务健康、事件和 C 服务日志。'
} > "${output_dir}/README.md"

echo "catalog fault experiment passed; evidence: ${output_dir}"
