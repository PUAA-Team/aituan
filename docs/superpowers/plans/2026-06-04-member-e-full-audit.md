# Member E Full Audit And Repair Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Comprehensively audit and repair all Member E owned features, including backend behavior, user app flows, merchant/admin web pages, and visual usability defects.

**Architecture:** Audit from source of truth outward: database and backend services first, then API consumers, then manual GUI flows. Each bug must be reproduced or evidenced before repair, and each repair must add or extend a regression check.

**Tech Stack:** Spring Boot 3 / Java 17 / Maven / Flyway / H2 tests, Flutter user app on macOS Apple Silicon, Vue 3 + Vite merchant/admin web, Bash regression script, Computer Use for GUI inspection.

---

## Scope

Member E owned scope:

- Review and rating: publish reviews, merchant detail review cards, my reviews, review detail, helpful vote, review report, report evidence images, merchant average rating.
- Support/customer service: user support session list, chat UI, merchant support queue, platform support intervention, keyword replies, human handoff, message source distinction.
- Complaints: user complaint list/submit/detail/supplement, complaint evidence images, merchant complaint ticket list/detail, admin complaint handling.
- Merchant console: review management, support sessions, complaint tickets, data overview naming and responsive layout.
- Admin governance: review audit, complaint tickets, platform support queue, audit logs and governance dashboard.
- Data and verification: migrations, seed demo data, file upload whitelist, audit logs, regression script.

Out of scope unless discovered as a Member E blocker:

- Payment/order creation internals, delivery assignment, voucher verification, merchant catalog editing, search/recommendation logic.

## Files To Inspect

Backend:

- `services/backend/src/main/java/com/aituan/interaction/InteractionService.java`
- `services/backend/src/main/java/com/aituan/interaction/InteractionRepository.java`
- `services/backend/src/main/java/com/aituan/support/SupportService.java`
- `services/backend/src/main/java/com/aituan/support/SupportRepository.java`
- `services/backend/src/main/java/com/aituan/support/AiSupportService.java`
- `services/backend/src/main/java/com/aituan/complaint/ComplaintService.java`
- `services/backend/src/main/java/com/aituan/complaint/ComplaintRepository.java`
- `services/backend/src/main/java/com/aituan/common/file/FileStorageService.java`
- `database/migrations/V003__init_trade_review_message.sql`
- `database/migrations/V011__support_platform_handoff.sql`
- `database/migrations/V012__review_report_evidence.sql`
- `database/migrations/V013__merchant_support_auto_reply_rules.sql`
- `database/seeds/R__seed_demo_data.sql`

Backend tests:

- `services/backend/src/test/java/com/aituan/interaction/InteractionServiceTest.java`
- `services/backend/src/test/java/com/aituan/support/SupportServiceTest.java`
- `services/backend/src/test/java/com/aituan/complaint/ComplaintServiceTest.java`

User app:

- `apps/user_app/lib/features/merchant/presentation/takeaway_merchant_sections.dart`
- `apps/user_app/lib/features/merchant/presentation/service_merchant_page.dart`
- `apps/user_app/lib/features/review/presentation/review_publish_page.dart`
- `apps/user_app/lib/features/review/presentation/my_reviews_page.dart`
- `apps/user_app/lib/features/review/presentation/review_detail_page.dart`
- `apps/user_app/lib/features/review/data/review_repository.dart`
- `apps/user_app/lib/features/support/presentation/support_sessions_page.dart`
- `apps/user_app/lib/features/support/presentation/support_chat_page.dart`
- `apps/user_app/lib/features/support/data/support_repository.dart`
- `apps/user_app/lib/features/complaint/presentation/complaint_list_page.dart`
- `apps/user_app/lib/features/complaint/presentation/complaint_submit_page.dart`
- `apps/user_app/lib/features/complaint/presentation/complaint_detail_page.dart`
- `apps/user_app/lib/features/complaint/data/complaint_repository.dart`
- `apps/user_app/lib/features/order/presentation/takeaway_order_detail_page.dart`
- `apps/user_app/lib/features/order/presentation/service_order_detail_page.dart`
- `apps/user_app/lib/features/message/presentation/message_page.dart`

Merchant web:

- `apps/merchant_web/src/pages/DashboardPage.vue`
- `apps/merchant_web/src/pages/ReviewPage.vue`
- `apps/merchant_web/src/pages/SessionsPage.vue`
- `apps/merchant_web/src/pages/ComplaintsPage.vue`
- `apps/merchant_web/src/components/ConsoleFrame.vue`
- `apps/merchant_web/src/api.ts`
- `apps/merchant_web/src/types.ts`
- `apps/merchant_web/src/styles.css`

Admin web:

- `apps/admin_web/src/pages/DashboardPage.vue`
- `apps/admin_web/src/pages/ReviewsPage.vue`
- `apps/admin_web/src/pages/ComplaintsPage.vue`
- `apps/admin_web/src/pages/PlatformSupportPage.vue`
- `apps/admin_web/src/pages/AuditLogsPage.vue`
- `apps/admin_web/src/components/AdminFrame.vue`
- `apps/admin_web/src/api.ts`
- `apps/admin_web/src/types.ts`
- `apps/admin_web/src/styles.css`

Verification:

- `scripts/verify/member_e_regression_checks.sh`
- `docs/stage6-memberE/E修改.md`
- `docs/stage6-memberE/交付说明.md`

## Task 1: Baseline Static And Automated Checks

**Files:**
- Inspect: all files listed above
- Modify if defects are found: `scripts/verify/member_e_regression_checks.sh`
- Output evidence: terminal output and bug list in the final response

- [ ] **Step 1: Confirm git state**

Run:

```bash
git status --short --branch
git log -1 --oneline
```

Expected:

```text
## member-e-improvements...origin/member-e-improvements
e683e78 Fix merchant support rules and complaint views
```

If local changes exist, classify them before touching files.

- [ ] **Step 2: Run backend tests**

Run:

```bash
cd services/backend
mvn test
```

Expected:

```text
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Run user app checks**

Run:

```bash
cd apps/user_app
flutter analyze
flutter test
```

Expected:

```text
No issues found!
All tests passed!
```

- [ ] **Step 4: Run web builds**

Run:

```bash
cd apps/merchant_web
npm run build
cd ../admin_web
npm run build
```

Expected:

```text
✓ built
```

- [ ] **Step 5: Run Member E regression script**

Run:

```bash
bash scripts/verify/member_e_regression_checks.sh
```

Expected:

```text
member E regression checks passed
```

If a check is missing for a discovered bug, add a concrete grep or test check to this script before fixing the bug.

## Task 2: Backend Contract Audit

**Files:**
- Inspect/modify: `InteractionService.java`, `InteractionRepository.java`, `SupportService.java`, `SupportRepository.java`, `ComplaintService.java`, `ComplaintRepository.java`, `FileStorageService.java`
- Test/modify: `InteractionServiceTest.java`, `SupportServiceTest.java`, `ComplaintServiceTest.java`

- [ ] **Step 1: Audit review invariants**

Check these invariants in code and tests:

- A user can review only their own completed/used order.
- Rating is required and bounded to 1..5.
- One active review per order.
- Newly submitted reviews appear in merchant detail and my reviews.
- Review images and report evidence URLs are stored and returned.
- Helpful vote is idempotent per user.
- Duplicate active report is rejected or treated consistently.
- Merchant average rating uses published reviews and returns a readable unrated state when no reviews exist.

Run targeted tests:

```bash
cd services/backend
mvn -Dtest=InteractionServiceTest test
```

Expected:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 2: Audit support invariants**

Check these invariants in code and tests:

- User sessions cannot leak across users.
- Merchant sessions cannot leak across merchants.
- Platform sessions use platform sender labels and merchant sessions use merchant labels.
- Merchant auto reply rules are scoped by merchant id.
- Disabled merchant rules do not reply.
- Platform keyword reply does not call OpenAI or require `OPENAI_API_KEY`.
- User sending `转人工` or tapping handoff switches platform session to human mode.
- Once human mode is active, AI keyword replies stop.
- Merchant requesting platform intervention creates a distinguishable platform message.

Run:

```bash
cd services/backend
mvn -Dtest=SupportServiceTest test
```

Expected:

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Audit complaint invariants**

Check these invariants in code and tests:

- User complaint history is readable after submit.
- Profile entry opens history, order entry binds order.
- Evidence images are accepted with `bizType=complaint`.
- User can supplement a complaint.
- Merchant complaint list is scoped to that merchant only.
- Merchant detail cannot open another merchant's ticket.
- Admin complaint state flow is `pending -> accepted -> resolved -> closed`.
- Illegal transitions are rejected.

Run:

```bash
cd services/backend
mvn -Dtest=ComplaintServiceTest test
```

Expected:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Task 3: User App Flow And UI Audit

**Files:**
- Inspect/modify: all user app files listed above
- Test/modify: `apps/user_app/test/widget_test.dart`

- [ ] **Step 1: Review merchant detail evaluation UI**

Inspect `takeaway_merchant_sections.dart` and `service_merchant_page.dart` for:

- No placeholder review text.
- Review cards show user nickname, rating, content, tags, images, helpful count, merchant reply.
- Tapping a review opens `Routes.reviewDetail`.
- Empty review state is readable.
- Unrated stores show `暂无评分`, not a fake score.
- Long review text wraps without overlapping.

Command:

```bash
rg -n "review|评价|rating|暂无评分|Routes.reviewDetail|resolveAssetUrl" apps/user_app/lib/features/merchant/presentation
```

Expected: review detail routing and asset URL resolution are present.

- [ ] **Step 2: Review review publish/detail/report UI**

Inspect:

- `review_publish_page.dart`
- `my_reviews_page.dart`
- `review_detail_page.dart`

Checklist:

- Rating selector is not fixed at one value.
- Image picker/upload failures show an actionable error and do not crash.
- Submit button disables while uploading/submitting.
- Review detail shows images with stable size.
- Helpful and report buttons prevent duplicate taps.
- Report dialog supports evidence image upload and blocks submit while uploading.
- Text fits on small screens.

Command:

```bash
rg -n "_submitting|_uploading|_helpfulBusy|_reporting|bizType: 'report'|Image|Wrap|SingleChildScrollView" apps/user_app/lib/features/review
```

Expected: busy flags, upload handling, and scrollable layouts are present.

- [ ] **Step 3: Review support UI**

Inspect:

- `support_sessions_page.dart`
- `support_chat_page.dart`

Checklist:

- Merchant support and platform support are clearly separated.
- Chat bubbles use visible labels for `我`, `商家客服`, `平台客服`, and system messages.
- Enter/send action sends messages.
- Handoff and end buttons have normal readable colors.
- Complaint entry is easy to find.
- Returning from merchant/order support preserves a sensible back path.
- Long messages wrap and do not cover action buttons.

Command:

```bash
rg -n "商家客服|平台客服|isMerchant|isPlatform|TextInputAction.send|onSubmitted|handoff|投诉|Navigator" apps/user_app/lib/features/support
```

Expected: all interaction hooks are present.

- [ ] **Step 4: Review complaint UI**

Inspect:

- `complaint_list_page.dart`
- `complaint_submit_page.dart`
- `complaint_detail_page.dart`

Checklist:

- Profile complaint entry opens history, not direct submit.
- History shows submitted complaints and status.
- Submit from profile can choose an order.
- Submit from order has order/store context.
- Evidence upload displays previews and upload failures.
- Detail shows progress, result, evidence images, and supplement entry.
- Refresh callbacks do not accidentally return a `Future` from `setState`.

Command:

```bash
rg -n "选择订单|补充意见|evidence|image|setState|Routes.complaint" apps/user_app/lib/features/complaint apps/user_app/lib/features/profile apps/user_app/lib/features/order
```

Expected: complaint history, order selection, supplement, and image evidence paths are present.

## Task 4: Merchant Web UI Audit

**Files:**
- Inspect/modify: `apps/merchant_web/src/pages/*.vue`, `ConsoleFrame.vue`, `api.ts`, `types.ts`, `styles.css`

- [ ] **Step 1: Review navigation and responsive shell**

Checklist:

- First nav label is not `驾驶舱`; it should be `经营概览` or similar.
- Sidebar/nav does not squeeze content on narrow widths.
- Active page title matches content.
- Mobile/tablet widths do not create horizontal overflow.

Command:

```bash
rg -n "驾驶舱|经营概览|投诉工单|客服会话|评价管理|@media|grid-template|overflow" apps/merchant_web/src
```

Expected: no `驾驶舱` in merchant UI; responsive CSS exists.

- [ ] **Step 2: Review merchant review management**

Checklist:

- Rating display is based on actual rating, not fixed text.
- Long comments and replies wrap.
- Merchant reply action is clearly disabled or hidden when already replied.
- Empty/loading/error states are readable.
- Review evidence images render with stable dimensions.

Command:

```bash
rg -n "ratingText|reply|回复|img|empty|loading|error|overflow-wrap" apps/merchant_web/src/pages/ReviewPage.vue apps/merchant_web/src/styles.css
```

Expected: bounded rating display and image handling exist.

- [ ] **Step 3: Review merchant support page**

Checklist:

- Session list, messages, templates, and auto reply rules are not crowded.
- Auto reply form validates keywords and reply content.
- Editing an existing rule is visually clear.
- Delete action has confirmation.
- Platform intervention entry is clear and not destructive.
- Complaint ticket entry opens merchant complaint page, not admin page.
- Long messages do not overflow cards.

Command:

```bash
rg -n "autoReplyRules|ruleForm|confirm|requestPlatformIntervention|openComplaints|投诉工单|overflow-wrap" apps/merchant_web/src/pages/SessionsPage.vue
```

Expected: editable rule controls and merchant complaint routing exist.

- [ ] **Step 4: Review merchant complaint page**

Checklist:

- List filters work for status/order/store.
- Detail panel shows ticket number, order number, store, status, content, evidence images, and logs.
- Empty states guide the user without filler copy.
- Images use `resolveAssetUrl`.
- Detail panel is usable on small desktop widths.

Command:

```bash
rg -n "status|orderNo|storeName|resolveAssetUrl|evidence|logs|empty|detail" apps/merchant_web/src/pages/ComplaintsPage.vue
```

Expected: filters, detail, logs, and evidence rendering exist.

## Task 5: Admin Web UI Audit

**Files:**
- Inspect/modify: `apps/admin_web/src/pages/ReviewsPage.vue`, `ComplaintsPage.vue`, `PlatformSupportPage.vue`, `AuditLogsPage.vue`, `DashboardPage.vue`, `api.ts`, `types.ts`, `styles.css`

- [ ] **Step 1: Review admin review audit**

Checklist:

- Rating display is bounded and not fake.
- Reported filter uses active reports.
- Report evidence images render.
- Pass/hide/restore actions have clear state and do not allow impossible transitions.
- Long report reasons wrap.

Command:

```bash
rg -n "ratingText|reported|reportEvidenceUrls|hide|restore|pass|img|overflow-wrap" apps/admin_web/src/pages/ReviewsPage.vue
```

Expected: report evidence and audit actions are present.

- [ ] **Step 2: Review admin complaints**

Checklist:

- Filters accept support entry `orderNo` and `storeName`.
- State action buttons match backend transition rules.
- Detail view shows complaint logs and evidence images.
- Closed tickets cannot be acted on incorrectly.

Command:

```bash
rg -n "orderNoKeyword|storeNameKeyword|accept|resolve|close|evidence|logs|disabled" apps/admin_web/src/pages/ComplaintsPage.vue apps/admin_web/src/api.ts
```

Expected: filters and state actions are present.

- [ ] **Step 3: Review platform support**

Checklist:

- Queue distinguishes AI mode and human mode.
- Human reply marks session as human mode.
- End/close controls are readable.
- Messages distinguish user, merchant, platform, AI, and system.
- Complaint-related handoff flow can be located.

Command:

```bash
rg -n "assistant|human|sendPlatformSupportMessage|close|平台|商家|AI|system" apps/admin_web/src/pages/PlatformSupportPage.vue
```

Expected: human reply and mode display are present.

- [ ] **Step 4: Review dashboard and audit logs**

Checklist:

- Governance counts match backend fields.
- Audit log page can filter or scan meaningful actions.
- Empty/loading/error states are not visually broken.
- Tables remain readable on laptop width.

Command:

```bash
rg -n "pendingReviews|pendingComplaints|openSessions|audit|loading|empty|table|@media" apps/admin_web/src/pages apps/admin_web/src/styles.css
```

Expected: dashboard and audit log state fields are present.

## Task 6: Manual GUI Inspection On macOS Apple Silicon

**Files:**
- No source edits unless defects are found.
- Use Computer Use for Chrome/Android Emulator visual inspection.

- [ ] **Step 1: Start backend**

Run:

```bash
cd services/backend
mvn spring-boot:run
```

Expected:

```text
Started AituanApplication
```

If port `8080` is occupied, identify the process before changing ports.

- [ ] **Step 2: Start merchant and admin web**

Run merchant web:

```bash
cd apps/merchant_web
npm run dev -- --host 127.0.0.1 --port 5173
```

Run admin web:

```bash
cd apps/admin_web
npm run dev -- --host 127.0.0.1 --port 5174
```

Expected:

```text
Local: http://127.0.0.1:5173/
Local: http://127.0.0.1:5174/
```

- [ ] **Step 3: Inspect merchant web in Chrome**

Use account `demo_merchant / 123456`.

Pages to click through:

- `经营概览`
- `评价管理`
- `客服会话`
- `投诉工单`

Visual checklist:

- No clipped Chinese text.
- No horizontal overflow at normal laptop width.
- Buttons have visible enabled/disabled colors.
- Tables do not force tiny unreadable columns.
- Forms have clear labels and error feedback.
- Empty/detail states are not confusing.

- [ ] **Step 4: Inspect admin web in Chrome**

Use account `demo_admin / 123456`.

Pages to click through:

- `平台总览`
- `评价审核`
- `投诉工单`
- `平台客服`
- `审计日志`

Visual checklist:

- Review and complaint evidence images render.
- State transition buttons match current status.
- Platform support message list labels each sender clearly.
- Modal/detail panels fit on screen.

- [ ] **Step 5: Inspect Flutter app on Android emulator**

Because this Mac is Apple Silicon, use an arm64 Android emulator image. First list devices:

```bash
cd apps/user_app
flutter devices
```

If no emulator is running, start Android Studio's Device Manager and choose an Apple Silicon compatible virtual device.

Run:

```bash
flutter run -d <emulator-id> --dart-define=AITUAN_API_BASE=http://10.0.2.2:8080
```

Use account `demo_user / 123456`.

Screens to inspect:

- Merchant detail review tab/card area.
- Order detail `联系商家` and `投诉/反馈` actions.
- Review publish page.
- My reviews page.
- Review detail page with helpful/report.
- Support session list.
- Support chat page with merchant and platform messages.
- Complaint history.
- Complaint submit.
- Complaint detail supplement.
- Message page support entry.

Visual checklist:

- Buttons are not too small.
- Important actions are visible without hunting.
- Chat messages wrap and show correct source.
- Images load or show stable fallback.
- Back navigation returns to a sensible previous screen.
- Forms do not hide behind the keyboard.

## Task 7: Reproduce And Fix Any Discovered Bugs

**Files:**
- Modify only the files directly responsible for each confirmed bug.
- Add tests to backend test files or regression script for each bug.

- [ ] **Step 1: Record each bug as a reproducible case**

For each issue, write:

```text
Bug:
Area:
Reproduction:
Expected:
Actual:
Root cause:
Fix file(s):
Verification:
```

Do not fix until `Root cause` is identified.

- [ ] **Step 2: Add or extend a failing automated check**

Backend behavior bug:

```bash
cd services/backend
mvn -Dtest=<TargetTestClass>#<newTestName> test
```

Expected before fix:

```text
Failures: 1
```

Frontend structural bug:

```bash
bash scripts/verify/member_e_regression_checks.sh
```

Expected before fix:

```text
FAIL: <specific message>
```

- [ ] **Step 3: Implement minimal fix**

Use existing repository patterns:

- Backend: service validates, repository persists/queries, controller maps role endpoints.
- Flutter: repository maps DTOs; page widgets keep stable scrollable layouts; busy flags prevent duplicate actions.
- Vue: API layer owns fetch calls; pages keep clear loading/error/empty states; CSS handles responsive layout.

- [ ] **Step 4: Verify targeted fix**

Run the failing check again.

Expected:

```text
BUILD SUCCESS
```

or:

```text
member E regression checks passed
```

- [ ] **Step 5: Run full verification**

Run:

```bash
cd services/backend && mvn test
cd ../../apps/user_app && flutter analyze && flutter test
cd ../merchant_web && npm run build
cd ../admin_web && npm run build
cd ../.. && bash scripts/verify/member_e_regression_checks.sh
git diff --check
```

Expected:

```text
BUILD SUCCESS
No issues found!
All tests passed!
✓ built
member E regression checks passed
```

## Task 8: Final Evidence And Push

**Files:**
- Inspect: `git status`
- Modify: none unless final documentation is requested

- [ ] **Step 1: Summarize defects**

Prepare a concise list:

```text
Fixed:
- <bug> -> <file> -> <verification>

No defect found:
- <area> -> <evidence>

Residual risk:
- <manual limitation, if any>
```

- [ ] **Step 2: Commit**

Run:

```bash
git status --short
git add <changed-files>
git commit -m "Fix member E audit issues"
```

Expected:

```text
[member-e-improvements <hash>] Fix member E audit issues
```

- [ ] **Step 3: Push**

Run:

```bash
GIT_TERMINAL_PROMPT=0 git -c http.proxy=http://127.0.0.1:7892 -c https.proxy=http://127.0.0.1:7892 push origin member-e-improvements
git ls-remote origin refs/heads/member-e-improvements
```

Expected:

```text
member-e-improvements -> member-e-improvements
<commit-hash> refs/heads/member-e-improvements
```

## Self-Review

- Spec coverage: covers all items from `docs/stage6-memberE/E修改.md` and the delivered Member E scope in `docs/stage6-memberE/交付说明.md`.
- Placeholder scan: no task depends on unspecified files or commands.
- Type consistency: file and command names match current repository structure.
- macOS/Apple Silicon: Android validation uses `flutter devices` and an arm64 emulator instead of Windows scripts.
