#!/usr/bin/env bash
set -euo pipefail

domain="${AITUAN_DOMAIN:-aituan.2b.gs}"
namespace="${AITUAN_NAMESPACE:-aituan}"
letsencrypt_dir="${AITUAN_LETSENCRYPT_DIR:-/etc/letsencrypt}"
certificate="${letsencrypt_dir}/live/${domain}/fullchain.pem"
private_key="${letsencrypt_dir}/live/${domain}/privkey.pem"

test -r "${certificate}"
test -r "${private_key}"

kubectl -n "${namespace}" create secret tls aituan-tls \
  --cert="${certificate}" \
  --key="${private_key}" \
  --dry-run=client -o yaml \
  | kubectl apply -f -

if kubectl -n "${namespace}" get deployment aituan-web >/dev/null 2>&1; then
  kubectl -n "${namespace}" rollout restart deployment/aituan-web
  kubectl -n "${namespace}" rollout status deployment/aituan-web --timeout=600s
fi
