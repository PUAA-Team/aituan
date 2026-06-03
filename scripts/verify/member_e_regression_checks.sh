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

if ! grep -q "平台客服" "$ROOT/apps/user_app/lib/features/support/presentation/support_sessions_page.dart"; then
  fail "support sessions should expose platform support"
fi

if ! grep -q "handoffToHuman" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat should expose platform AI to human handoff"
fi

if ! grep -q "Routes.complaintSubmit" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat should expose complaint entry"
fi

if ! grep -q "商家客服" "$ROOT/apps/user_app/lib/features/support/presentation/support_sessions_page.dart"; then
  fail "support sessions should expose merchant support"
fi

if ! grep -q "选择订单" "$ROOT/apps/user_app/lib/features/complaint/presentation/complaint_submit_page.dart"; then
  fail "complaint submit should let profile entry choose an order"
fi

if ! grep -q "补充意见" "$ROOT/apps/user_app/lib/features/complaint/presentation/complaint_detail_page.dart"; then
  fail "complaint detail should support user supplements"
fi

if grep -q "setState(() => _future = complaintRepository.fetchMy" "$ROOT/apps/user_app/lib/features/complaint/presentation/complaint_list_page.dart"; then
  fail "complaint list setState callback must not return the fetch Future"
fi

if ! grep -q "关键词自动回复" "$ROOT/apps/merchant_web/src/pages/SessionsPage.vue"; then
  fail "merchant sessions should show keyword auto reply rules"
fi

if ! grep -q "requestPlatformIntervention" "$ROOT/apps/merchant_web/src/pages/SessionsPage.vue"; then
  fail "merchant sessions should support platform intervention"
fi

if ! grep -q "投诉工单" "$ROOT/apps/merchant_web/src/pages/SessionsPage.vue"; then
  fail "merchant sessions should expose complaint ticket entry"
fi

if ! grep -q "PlatformSupportPage" "$ROOT/apps/admin_web/src/App.vue"; then
  fail "admin web should expose platform support queue"
fi

if ! grep -q "sendPlatformSupportMessage" "$ROOT/apps/admin_web/src/pages/PlatformSupportPage.vue"; then
  fail "admin platform support page should send human replies"
fi

if ! grep -q "assistant_mode" "$ROOT/database/migrations/V011__support_platform_handoff.sql"; then
  fail "support handoff migration should include assistant mode"
fi

if ! grep -q "Routes.supportSessions" "$ROOT/apps/user_app/lib/features/message/presentation/message_page.dart"; then
  fail "message page should link to support sessions"
fi

printf 'member E regression checks passed\n'
