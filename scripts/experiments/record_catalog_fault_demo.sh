#!/usr/bin/env bash
set -Eeuo pipefail

namespace="${K8S_NAMESPACE:-aituan}"
origin="${FAULT_TEST_ORIGIN:-https://aituan.2b.gs}"
account="${FAULT_TEST_ACCOUNT:-demo_user}"
password="${FAULT_TEST_PASSWORD:-123456}"
catalog_service="merchant-catalog-service"
hpa_target=""
fault_injected=false
token=""

recover_catalog() {
  if [[ "${fault_injected}" != true ]]; then
    return
  fi
  echo "[恢复] 重新绑定 HPA，并把商品服务恢复为 1 个副本。"
  kubectl -n "${namespace}" patch "hpa/${catalog_service}" --type=merge \
    -p "{\"spec\":{\"scaleTargetRef\":{\"name\":\"${hpa_target}\"}}}" >/dev/null 2>&1 || true
  kubectl -n "${namespace}" scale "deployment/${catalog_service}" --replicas=1 >/dev/null 2>&1 || true
  kubectl -n "${namespace}" rollout status "deployment/${catalog_service}" --timeout=300s || true
  fault_injected=false
}

on_exit() {
  exit_code=$?
  recover_catalog
  if [[ "${exit_code}" -ne 0 ]]; then
    echo "脚本异常退出，但已执行商品服务恢复。" >&2
  fi
  exit "${exit_code}"
}
trap on_exit EXIT INT TERM HUP

pause_for_browser() {
  local prompt="$1"
  if [[ "${DEMO_AUTO_ADVANCE:-false}" = true ]]; then
    sleep "${DEMO_AUTO_PAUSE_SECONDS:-1}"
    return
  fi
  read -r -p "${prompt}" _
}

api_request() {
  local method="$1" path="$2" body="${3:-}"
  local -a arguments=(-ksS --max-time 12 -X "${method}" -H "Authorization: Bearer ${token}")
  if [[ -n "${body}" ]]; then
    arguments+=(-H 'Content-Type: application/json' --data "${body}")
  fi
  curl "${arguments[@]}" "${origin}${path}"
}

for command_name in kubectl jq curl; do
  command -v "${command_name}" >/dev/null || {
    echo "缺少命令：${command_name}" >&2
    exit 1
  }
done

kubectl get nodes >/dev/null
for service in api-gateway identity-asset-service merchant-catalog-service trade-fulfillment-service engagement-platform-service; do
  kubectl -n "${namespace}" rollout status "deployment/${service}" --timeout=300s >/dev/null
done
hpa_target="$(kubectl -n "${namespace}" get "hpa/${catalog_service}" -o jsonpath='{.spec.scaleTargetRef.name}')"
if [[ "${hpa_target}" != "${catalog_service}" ]]; then
  echo "HPA 当前目标异常：${hpa_target}，请先恢复后再录制。" >&2
  exit 1
fi

login_response="$(curl -ksS --max-time 12 -H 'Content-Type: application/json' \
  --data "{\"account\":\"${account}\",\"password\":\"${password}\"}" \
  "${origin}/api/open/auth/user/login/password")"
token="$(jq -r '.data.accessToken // .data.token // empty' <<<"${login_response}")"
if [[ -z "${token}" ]]; then
  echo "演示账号登录失败。" >&2
  exit 1
fi

echo "[步骤 1/3] 建立正常购物车快照。"
api_request DELETE '/api/app/trade/cart?storeId=1' >/dev/null
baseline="$(api_request POST '/api/app/trade/cart/items' '{"storeId":1,"itemId":1002,"quantity":2}')"
jq -e '.code == 0 and .data.catalogAvailable == true and (.data.items | length) == 1' \
  <<<"${baseline}" >/dev/null
jq '{"状态":"商品服务正常","门店":.data.storeName,"商品":.data.items[0].itemName,"数量":.data.items[0].quantity,"金额":.data.amount}' \
  <<<"${baseline}"
kubectl -n "${namespace}" get deployment/${catalog_service} hpa/${catalog_service}
pause_for_browser $'请在前端登录 demo_user，进入“塔斯汀中国汉堡”并打开购物车；确认商品 ×2 后，回车注入故障：'

echo "[步骤 2/3] 将商品服务从 1 个副本缩到 0。"
fault_injected=true
kubectl -n "${namespace}" patch "hpa/${catalog_service}" --type=merge \
  -p '{"spec":{"scaleTargetRef":{"name":"merchant-catalog-service-recording-paused"}}}' >/dev/null
kubectl -n "${namespace}" scale "deployment/${catalog_service}" --replicas=0 >/dev/null
for attempt in $(seq 1 30); do
  replicas="$(kubectl -n "${namespace}" get "deployment/${catalog_service}" -o jsonpath='{.status.replicas}')"
  [[ "${replicas:-0}" = 0 ]] && break
  [[ "${attempt}" != 30 ]] || {
    echo "商品服务未能在预期时间内停止。" >&2
    exit 1
  }
  sleep 2
done
kubectl -n "${namespace}" get deployment/${catalog_service} deployment/trade-fulfillment-service hpa/${catalog_service}

degraded="$(api_request GET '/api/app/trade/cart?storeId=1')"
jq -e '.code == 0 and .data.catalogAvailable == false and (.data.items | length) == 1' \
  <<<"${degraded}" >/dev/null
jq '{"状态":"已返回备用快照","catalogAvailable":.data.catalogAvailable,"提示":.data.notice,"商品":.data.items[0].itemName,"数量":.data.items[0].quantity,"金额":.data.amount}' \
  <<<"${degraded}"

profile_code="$(api_request GET '/api/app/account/profile' | jq -r '.code')"
orders_code="$(api_request GET '/api/app/trade/orders?page=1&pageSize=20' | jq -r '.code')"
reviews_code="$(api_request GET '/api/app/interaction/stores/1/reviews?page=1&pageSize=20' | jq -r '.code')"
printf '其他业务检查：账号 A code=%s，交易 C code=%s，互动 D code=%s\n' \
  "${profile_code}" "${orders_code}" "${reviews_code}"
test "${profile_code}" = 0
test "${orders_code}" = 0
test "${reviews_code}" = 0

pause_for_browser $'请切到前端刷新：拍黄色备用结果、禁用结算、“移除商品”和其他页面正常；完成后回车恢复 B：'

echo "[步骤 3/3] 恢复商品服务。"
recover_catalog
for attempt in $(seq 1 30); do
  recovery="$(api_request GET '/api/app/trade/cart?storeId=1')"
  if jq -e '.code == 0 and .data.catalogAvailable == true' <<<"${recovery}" >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" != 30 ]] || {
    echo "商品服务恢复后，购物车仍未回到正常状态。" >&2
    exit 1
  }
  sleep 2
done
kubectl -n "${namespace}" get deployment/${catalog_service} deployment/trade-fulfillment-service hpa/${catalog_service}
jq '{"状态":"商品服务已恢复","catalogAvailable":.data.catalogAvailable,"商品数量":(.data.items | length),"金额":.data.amount}' \
  <<<"${recovery}"
pause_for_browser $'请在前端点击“重新检测”并拍到黄色提示消失、新增恢复；完成后回车清理演示购物车：'

api_request DELETE '/api/app/trade/cart?storeId=1' >/dev/null
echo "录制流程完成：B 与 HPA 已恢复，demo_user 的演示购物车已清空。"
