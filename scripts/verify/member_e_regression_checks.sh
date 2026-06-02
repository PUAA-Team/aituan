#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

if grep -R "驾驶舱" "$ROOT/apps/merchant_web/src" >/dev/null; then
  fail "merchant web should use 经营概览/数据概览 instead of 驾驶舱"
fi

if grep -q "Routes.complaintSubmit" "$ROOT/apps/user_app/lib/features/profile/presentation/profile_page.dart"; then
  fail "profile complaint entry should open complaint history, not submit directly"
fi

if grep -q "主题：" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat page should not display a topic card"
fi

if ! grep -q "联系商家" "$ROOT/apps/user_app/lib/features/order/presentation/takeaway_order_detail_page.dart"; then
  fail "takeaway order detail should expose a visible 联系商家 action"
fi

if grep -q "汉堡现做热乎" "$ROOT/apps/user_app/lib/features/merchant/presentation/takeaway_merchant_sections.dart"; then
  fail "merchant review panel should not use hard-coded placeholder review text"
fi

if ! grep -q "暂无评分" "$ROOT/apps/user_app/lib/features/merchant/presentation"/*.dart; then
  fail "merchant presentation should include an unrated display state"
fi

printf 'member E regression checks passed\n'
