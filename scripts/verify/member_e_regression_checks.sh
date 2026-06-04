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

if ! grep -q "TextInputAction.send" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat should submit messages with the keyboard send action"
fi

if ! grep -q "onSubmitted" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat should send messages when pressing enter"
fi

if grep -q "TextStyle(color: Colors.white)" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "support chat app bar actions should not hard-code white text"
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

if ! grep -q "markHumanHandoff(sessionId)" "$ROOT/services/backend/src/main/java/com/aituan/support/SupportService.java"; then
  fail "admin platform support replies should switch AI sessions to human mode"
fi

if ! grep -q '!\"ai\".equals(row.assistantMode())' "$ROOT/services/backend/src/main/java/com/aituan/support/SupportService.java"; then
  fail "platform human sessions should not continue keyword auto replies"
fi

if ! grep -q "orderNoKeyword" "$ROOT/apps/admin_web/src/pages/ComplaintsPage.vue"; then
  fail "admin complaints page should read support entry order number filter"
fi

if ! grep -q "storeNameKeyword" "$ROOT/apps/admin_web/src/pages/ComplaintsPage.vue"; then
  fail "admin complaints page should read support entry store name filter"
fi

if ! grep -q "query.set('orderNo'" "$ROOT/apps/admin_web/src/api.ts"; then
  fail "admin complaints API should pass orderNo filter"
fi

if ! grep -q "query.set('storeName'" "$ROOT/apps/admin_web/src/api.ts"; then
  fail "admin complaints API should pass storeName filter"
fi

if grep -q "/v1/responses\\|OPENAI_API_KEY\\|RestClient" "$ROOT/services/backend/src/main/java/com/aituan/support/AiSupportService.java"; then
  fail "platform assistant should use local keyword replies instead of OpenAI calls"
fi

if ! grep -q "case \"avatar\", \"store\", \"item\", \"announcement\", \"seed\", \"merchant-certification\"," "$ROOT/services/backend/src/main/java/com/aituan/common/file/FileStorageService.java"; then
  fail "file storage biz type whitelist structure changed unexpectedly"
fi

if ! grep -q "\"review\", \"complaint\", \"report\"" "$ROOT/services/backend/src/main/java/com/aituan/common/file/FileStorageService.java"; then
  fail "review/complaint/report image uploads should be accepted"
fi

if ! grep -q "bizType: 'report'" "$ROOT/apps/user_app/lib/features/review/presentation/review_detail_page.dart"; then
  fail "review report dialog should upload report evidence images"
fi

if ! grep -q "_helpfulBusy" "$ROOT/apps/user_app/lib/features/review/presentation/review_detail_page.dart"; then
  fail "review helpful action should prevent duplicate taps"
fi

if ! grep -q "_reporting" "$ROOT/apps/user_app/lib/features/review/presentation/review_detail_page.dart"; then
  fail "review report action should prevent duplicate submissions"
fi

if ! grep -q "_uploading ? null : _submit" "$ROOT/apps/user_app/lib/features/review/presentation/review_detail_page.dart"; then
  fail "review report dialog should block submit while evidence is uploading"
fi

if ! grep -q "ratingText" "$ROOT/apps/merchant_web/src/pages/ReviewPage.vue"; then
  fail "merchant review page should render non-fixed full rating text"
fi

if ! grep -q "ratingText" "$ROOT/apps/admin_web/src/pages/ReviewsPage.vue"; then
  fail "admin review page should render bounded rating text"
fi

if ! grep -q "reportEvidenceUrls" "$ROOT/apps/admin_web/src/pages/ReviewsPage.vue"; then
  fail "admin review page should show report evidence images"
fi

if ! grep -q "findActiveReport" "$ROOT/services/backend/src/main/java/com/aituan/interaction/InteractionRepository.java"; then
  fail "review reports should detect duplicate active reports"
fi

if ! grep -q "activeReportExistsSql" "$ROOT/services/backend/src/main/java/com/aituan/interaction/InteractionRepository.java"; then
  fail "reported filters should use active report existence"
fi

if ! grep -q "refreshReportedCount" "$ROOT/services/backend/src/main/java/com/aituan/interaction/InteractionService.java"; then
  fail "review audit/report flow should refresh reported counts"
fi

if ! grep -q "assistant_mode" "$ROOT/database/migrations/V011__support_platform_handoff.sql"; then
  fail "support handoff migration should include assistant mode"
fi

if ! grep -q "Routes.supportSessions" "$ROOT/apps/user_app/lib/features/message/presentation/message_page.dart"; then
  fail "message page should link to support sessions"
fi

printf 'member E regression checks passed\n'
