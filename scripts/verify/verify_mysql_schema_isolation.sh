#!/usr/bin/env bash
set -euo pipefail

mysql_host="${AITUAN_MYSQL_HOST:-127.0.0.1}"
mysql_port="${AITUAN_MYSQL_PORT:-3306}"

: "${AITUAN_IDENTITY_DB_PASSWORD:?AITUAN_IDENTITY_DB_PASSWORD is required}"
: "${AITUAN_MERCHANT_DB_PASSWORD:?AITUAN_MERCHANT_DB_PASSWORD is required}"
: "${AITUAN_TRADE_DB_PASSWORD:?AITUAN_TRADE_DB_PASSWORD is required}"
: "${AITUAN_PLATFORM_DB_PASSWORD:?AITUAN_PLATFORM_DB_PASSWORD is required}"

schema_user() {
  case "$1" in
    identity) printf '%s' "${AITUAN_IDENTITY_DB_USERNAME:-aituan_identity_svc}" ;;
    merchant) printf '%s' "${AITUAN_MERCHANT_DB_USERNAME:-aituan_merchant_svc}" ;;
    trade) printf '%s' "${AITUAN_TRADE_DB_USERNAME:-aituan_trade_svc}" ;;
    platform) printf '%s' "${AITUAN_PLATFORM_DB_USERNAME:-aituan_platform_svc}" ;;
    *) return 1 ;;
  esac
}

schema_password() {
  case "$1" in
    identity) printf '%s' "${AITUAN_IDENTITY_DB_PASSWORD}" ;;
    merchant) printf '%s' "${AITUAN_MERCHANT_DB_PASSWORD}" ;;
    trade) printf '%s' "${AITUAN_TRADE_DB_PASSWORD}" ;;
    platform) printf '%s' "${AITUAN_PLATFORM_DB_PASSWORD}" ;;
    *) return 1 ;;
  esac
}

schemas=(identity merchant trade platform)
for owner in "${schemas[@]}"; do
  own_schema="aituan_${owner}"
  owner_user="$(schema_user "${owner}")"
  owner_password="$(schema_password "${owner}")"
  own_table_count="$(MYSQL_PWD="${owner_password}" mysql \
    --protocol=TCP -h "${mysql_host}" -P "${mysql_port}" -u "${owner_user}" -N \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${own_schema}'")"
  if [[ ! "${own_table_count}" =~ ^[0-9]+$ || "${own_table_count}" -le 0 ]]; then
    echo "${owner} account cannot read its own schema or the schema has no tables" >&2
    exit 1
  fi

  for target in "${schemas[@]}"; do
    [[ "${target}" == "${owner}" ]] && continue
    if MYSQL_PWD="${owner_password}" mysql \
      --protocol=TCP -h "${mysql_host}" -P "${mysql_port}" -u "${owner_user}" \
      -e "SHOW TABLES FROM aituan_${target}" >/dev/null 2>&1; then
      echo "${owner} account unexpectedly accessed aituan_${target}" >&2
      exit 1
    fi
  done
done

if [[ -n "${AITUAN_MYSQL_ROOT_PASSWORD:-}" ]]; then
  cross_schema_fk_count="$(MYSQL_PWD="${AITUAN_MYSQL_ROOT_PASSWORD}" mysql \
    --protocol=TCP -h "${mysql_host}" -P "${mysql_port}" -u "${AITUAN_MYSQL_ROOT_USERNAME:-root}" -N \
    -e "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA IN ('aituan_identity','aituan_merchant','aituan_trade','aituan_platform') AND REFERENCED_TABLE_SCHEMA IS NOT NULL AND REFERENCED_TABLE_SCHEMA <> TABLE_SCHEMA")"
  if [[ "${cross_schema_fk_count}" != "0" ]]; then
    echo "Found ${cross_schema_fk_count} cross-schema foreign keys" >&2
    exit 1
  fi
fi

echo "Four-schema ownership and all 12 cross-schema denials passed"
