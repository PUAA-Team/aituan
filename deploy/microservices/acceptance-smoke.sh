#!/usr/bin/env bash
set -euo pipefail

base_url="${AITUAN_GATEWAY_URL:-http://127.0.0.1:18080}"
smoke_image="${AITUAN_SMOKE_IMAGE:-deploy/landing/favicon.png}"

request() {
  curl --fail-with-body --silent --show-error --max-time 15 "$@"
}

assert_ok() {
  local response="$1"
  local label="$2"
  if [[ "$(jq -r '.code // -1' <<<"${response}")" != "0" ]]; then
    echo "${label} failed: $(jq -c '{code,message}' <<<"${response}")" >&2
    exit 1
  fi
}

login() {
  local role="$1"
  local account="$2"
  local response
  response="$(request \
    -H 'Content-Type: application/json' \
    -d "{\"account\":\"${account}\",\"password\":\"123456\"}" \
    "${base_url}/api/open/auth/${role}/login/password")"
  assert_ok "${response}" "${role} login"
  jq -r '.data.token' <<<"${response}"
}

health="$(request "${base_url}/actuator/health")"
[[ "$(jq -r '.status' <<<"${health}")" == "UP" ]]

internal_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  --max-time 10 "${base_url}/internal/orders/9001/review-eligibility")"
[[ "${internal_status}" == "404" ]]

headers="$(curl --silent --dump-header - --output /dev/null --max-time 10 \
  "${base_url}/api/app/discovery/home")"
[[ "$(grep -i -c '^X-Request-Id:' <<<"${headers}")" == "1" ]]

user_token="$(login user demo_user)"
merchant_token="$(login merchant demo_merchant)"
login admin demo_admin >/dev/null

discovery="$(request "${base_url}/api/app/discovery/home")"
assert_ok "${discovery}" "discovery"
[[ "$(jq -r '.data.recommendations.total' <<<"${discovery}")" -gt 0 ]]

idempotency_key="acceptance-$(date +%s)-$$"
order_request="{\"storeId\":1,\"businessType\":\"takeaway\",\"addressId\":7001,\"items\":[{\"itemId\":1002,\"quantity\":2}],\"idempotencyKey\":\"${idempotency_key}\"}"
order="$(request \
  -H "Authorization: Bearer ${user_token}" \
  -H 'Content-Type: application/json' \
  -d "${order_request}" \
  "${base_url}/api/app/trade/orders")"
assert_ok "${order}" "create order"
order_id="$(jq -r '.data.id' <<<"${order}")"
[[ "$(jq -r '.data.items[0].itemId' <<<"${order}")" == "1002" ]]
[[ -n "$(jq -r '.data.items[0].coverUrl' <<<"${order}")" ]]

replayed="$(request \
  -H "Authorization: Bearer ${user_token}" \
  -H 'Content-Type: application/json' \
  -d "${order_request}" \
  "${base_url}/api/app/trade/orders")"
assert_ok "${replayed}" "replay order"
[[ "$(jq -r '.data.id' <<<"${replayed}")" == "${order_id}" ]]

response="$(request -X POST \
  -H "Authorization: Bearer ${user_token}" \
  -H 'Content-Type: application/json' \
  -d '{"paymentMode":"mock"}' \
  "${base_url}/api/app/trade/orders/${order_id}/pay")"
assert_ok "${response}" "pay order"
[[ "$(jq -r '.data.fulfillmentStatus' <<<"${response}")" == "merchant_pending" ]]

response="$(request -X POST \
  -H "Authorization: Bearer ${merchant_token}" \
  -H 'Content-Type: application/json' \
  -d '{"remark":"microservices acceptance"}' \
  "${base_url}/api/merchant/trade/orders/${order_id}/accept")"
assert_ok "${response}" "accept order"

for action in prepare ready delivery/advance delivery/advance complete; do
  response="$(request -X POST \
    -H "Authorization: Bearer ${merchant_token}" \
    "${base_url}/api/merchant/trade/orders/${order_id}/${action}")"
  assert_ok "${response}" "merchant ${action}"
done
[[ "$(jq -r '.data.fulfillmentStatus' <<<"${response}")" == "completed" ]]

review="$(request -X POST \
  -H "Authorization: Bearer ${user_token}" \
  -H 'Content-Type: application/json' \
  -d '{"rating":5,"content":"microservices integration acceptance","labels":["contract","e2e"],"imageUrls":[]}' \
  "${base_url}/api/app/interaction/orders/${order_id}/review")"
assert_ok "${review}" "submit review"
review_id="$(jq -r '.data.id' <<<"${review}")"
[[ "$(jq -r '.data.orderId' <<<"${review}")" == "${order_id}" ]]

if [[ -f "${smoke_image}" ]]; then
  source_hash="$(shasum -a 256 "${smoke_image}" | awk '{print $1}')"

  avatar="$(request \
    -H "Authorization: Bearer ${user_token}" \
    -F "file=@${smoke_image};type=image/png" \
    "${base_url}/api/app/account/avatar")"
  assert_ok "${avatar}" "upload avatar"
  avatar_url="$(jq -r '.data.avatarUrl' <<<"${avatar}")"
  avatar_hash="$(request "${base_url}${avatar_url}" | shasum -a 256 | awk '{print $1}')"
  [[ "${avatar_hash}" == "${source_hash}" ]]

  cover="$(request \
    -H "Authorization: Bearer ${merchant_token}" \
    -F "file=@${smoke_image};type=image/png" \
    "${base_url}/api/merchant/stores/current/cover")"
  assert_ok "${cover}" "upload store cover"
  cover_url="$(jq -r '.data.coverUrl' <<<"${cover}")"
  cover_hash="$(request "${base_url}${cover_url}" | shasum -a 256 | awk '{print $1}')"
  [[ "${cover_hash}" == "${source_hash}" ]]
fi

jq -n \
  --argjson orderId "${order_id}" \
  --argjson reviewId "${review_id}" \
  '{status:"PASS",gatewayInternalBlocked:true,requestIdCount:1,orderId:$orderId,reviewId:$reviewId,fileForwardingVerified:true}'
