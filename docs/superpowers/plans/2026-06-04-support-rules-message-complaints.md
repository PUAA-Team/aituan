# Support Rules, Message Source, and Merchant Complaints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix merchant-editable auto-reply rules, distinguish merchant and platform messages in the user support chat, and make the merchant-side complaint ticket entry open a correct merchant page.

**Architecture:** Add merchant-owned support auto-reply rules in the backend, expose them through merchant session APIs, and use them in merchant-scope auto replies. Keep platform AI keyword replies separate. Add a merchant complaint page inside the merchant console instead of opening the admin console. Update user chat rendering so `senderType=merchant` and `senderType=platform` have different labels and colors.

**Tech Stack:** Spring Boot 3, JDBC, Flyway SQL migrations, Vue 3 + TypeScript merchant console, Flutter user app, Maven tests, Vite builds, Flutter analyze/test.

---

## File Structure

**Backend**
- Modify: `database/migrations/V013__merchant_support_auto_reply_rules.sql`
  - New table `merchant_support_auto_reply_rule`.
- Modify: `services/backend/src/main/java/com/aituan/support/SupportDtos.java`
  - Add `SupportAutoReplyRuleView`, `SupportAutoReplyRuleUpsertRequest`.
- Modify: `services/backend/src/main/java/com/aituan/support/SupportRepository.java`
  - CRUD for merchant auto-reply rules and active rule matching.
- Modify: `services/backend/src/main/java/com/aituan/support/SupportService.java`
  - Merchant rule APIs and `autoReplyIfMatched` uses merchant rules before falling back to no reply.
- Modify: `services/backend/src/main/java/com/aituan/support/SupportMerchantController.java`
  - `GET/POST/PUT/DELETE /api/merchant/ops/sessions/auto-reply-rules`.
- Modify: `services/backend/src/test/java/com/aituan/support/SupportServiceTest.java`
  - Rule ownership, matching, update, delete tests.
- Modify: `services/backend/src/main/java/com/aituan/complaint/ComplaintDtos.java`
  - Reuse existing `ComplaintView`, no new DTO unless merchant action is added.
- Modify: `services/backend/src/main/java/com/aituan/complaint/ComplaintRepository.java`
  - Merchant-scoped complaint list/detail queries.
- Modify: `services/backend/src/main/java/com/aituan/complaint/ComplaintService.java`
  - `merchantTickets`, `merchantTicketDetail`.
- Create: `services/backend/src/main/java/com/aituan/complaint/ComplaintMerchantController.java`
  - Merchant complaint endpoints.
- Modify: `services/backend/src/test/java/com/aituan/complaint/ComplaintServiceTest.java`
  - Merchant can only see own complaints; detail includes correct ticket.

**Merchant Web**
- Modify: `apps/merchant_web/src/types.ts`
  - Add `SupportAutoReplyRuleView`, add `complaints` to `ConsolePage`, add complaint DTOs if not present.
- Modify: `apps/merchant_web/src/api.ts`
  - Add support rule CRUD and merchant complaint list/detail calls.
- Modify: `apps/merchant_web/src/pages/SessionsPage.vue`
  - Replace hard-coded `autoReplyRules` with editable rules UI.
  - Change complaint entry to navigate inside merchant console with filters.
- Create: `apps/merchant_web/src/pages/ComplaintsPage.vue`
  - Merchant ticket list/detail page.
- Modify: `apps/merchant_web/src/App.vue`
  - Route `activePage === 'complaints'`.
- Modify: `apps/merchant_web/src/components/ConsoleFrame.vue`
  - Add sidebar entry for `投诉工单`.

**User App**
- Modify: `apps/user_app/lib/features/support/presentation/support_chat_page.dart`
  - `_MessageBubble` distinguishes `user`, `merchant`, and `platform`.
- Modify: `apps/user_app/lib/features/support/data/support_repository.dart`
  - Keep `isPlatform`; add `isMerchant` helper for readability.

**Verification**
- Modify: `scripts/verify/member_e_regression_checks.sh`
  - Static checks for editable rules, user message source styling, merchant complaint page.

---

## Task 1: Backend Merchant Auto-Reply Rule Model

**Files:**
- Create: `database/migrations/V013__merchant_support_auto_reply_rules.sql`
- Modify: `services/backend/src/main/java/com/aituan/support/SupportDtos.java`
- Modify: `services/backend/src/main/java/com/aituan/support/SupportRepository.java`
- Modify: `services/backend/src/main/java/com/aituan/support/SupportService.java`
- Modify: `services/backend/src/main/java/com/aituan/support/SupportMerchantController.java`
- Test: `services/backend/src/test/java/com/aituan/support/SupportServiceTest.java`

- [ ] **Step 1: Write failing tests**

Add these tests to `SupportServiceTest`:

```java
@Test
void merchantCanCreateUpdateAndUseOwnAutoReplyRule() {
  TestAuthSupport.loginAsMerchant(2L);

  SupportAutoReplyRuleView created = supportService.createMerchantAutoReplyRule(
      new SupportAutoReplyRuleUpsertRequest("停车,车位", "门店附近有停车位，请按导航到店。", true));
  assertThat(created.keywords()).isEqualTo("停车,车位");
  assertThat(created.replyContent()).contains("停车位");
  assertThat(created.enabled()).isTrue();

  SupportAutoReplyRuleView updated = supportService.updateMerchantAutoReplyRule(
      created.id(),
      new SupportAutoReplyRuleUpsertRequest("排队,等位", "当前可能需要等位，请到店后取号。", true));
  assertThat(updated.keywords()).isEqualTo("排队,等位");

  SupportSessionView session = supportService.createUserSession(
      new SupportSessionCreateRequest(1L, "商家客服咨询", null));
  supportService.userSendMessage(session.id(), new SupportMessageCreateRequest("请问现在需要排队吗"));
  SupportSessionDetailView detail = supportService.userSessionDetail(session.id());

  assertThat(detail.messages())
      .anySatisfy(message -> {
        assertThat(message.senderType()).isEqualTo("merchant");
        assertThat(message.messageKind()).isEqualTo("auto_reply");
        assertThat(message.content()).contains("取号");
      });
}

@Test
void merchantDisabledAutoReplyRuleDoesNotReply() {
  TestAuthSupport.loginAsMerchant(2L);
  supportService.createMerchantAutoReplyRule(
      new SupportAutoReplyRuleUpsertRequest("停车", "有停车位", false));

  SupportSessionView session = supportService.createUserSession(
      new SupportSessionCreateRequest(1L, "商家客服咨询", null));
  supportService.userSendMessage(session.id(), new SupportMessageCreateRequest("停车方便吗"));
  SupportSessionDetailView detail = supportService.userSessionDetail(session.id());

  assertThat(detail.messages())
      .noneSatisfy(message -> assertThat(message.content()).isEqualTo("有停车位"));
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/services/backend
mvn -Dtest=SupportServiceTest test
```

Expected: compile failure because `SupportAutoReplyRuleView`, `SupportAutoReplyRuleUpsertRequest`, and service methods do not exist.

- [ ] **Step 3: Add migration**

Create `database/migrations/V013__merchant_support_auto_reply_rules.sql`:

```sql
CREATE TABLE IF NOT EXISTS merchant_support_auto_reply_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  keywords VARCHAR(255) NOT NULL,
  reply_content VARCHAR(500) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_support_auto_reply_merchant
  ON merchant_support_auto_reply_rule (merchant_id, enabled, is_deleted);
```

- [ ] **Step 4: Add DTOs**

Add to `SupportDtos.java`:

```java
record SupportAutoReplyRuleUpsertRequest(
    @NotBlank String keywords,
    @NotBlank String replyContent,
    Boolean enabled) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record SupportAutoReplyRuleView(
    Long id,
    Long merchantId,
    String keywords,
    String replyContent,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
```

- [ ] **Step 5: Add repository methods**

Add methods to `SupportRepository.java`:

```java
List<AutoReplyRuleRow> listAutoReplyRules(long merchantId) {
  return jdbcTemplate.query(
      """
      select id, merchant_id, keywords, reply_content, enabled, created_at, updated_at
      from merchant_support_auto_reply_rule
      where merchant_id = ? and is_deleted = 0
      order by id desc
      """,
      this::mapAutoReplyRule,
      merchantId);
}

List<AutoReplyRuleRow> listEnabledAutoReplyRules(long merchantId) {
  return jdbcTemplate.query(
      """
      select id, merchant_id, keywords, reply_content, enabled, created_at, updated_at
      from merchant_support_auto_reply_rule
      where merchant_id = ? and enabled = 1 and is_deleted = 0
      order by id desc
      """,
      this::mapAutoReplyRule,
      merchantId);
}

Optional<AutoReplyRuleRow> findAutoReplyRule(long id) {
  List<AutoReplyRuleRow> rows = jdbcTemplate.query(
      """
      select id, merchant_id, keywords, reply_content, enabled, created_at, updated_at
      from merchant_support_auto_reply_rule
      where id = ? and is_deleted = 0
      limit 1
      """,
      this::mapAutoReplyRule,
      id);
  return rows.stream().findFirst();
}

Long insertAutoReplyRule(long merchantId, String keywords, String replyContent, boolean enabled) {
  jdbcTemplate.update(
      """
      insert into merchant_support_auto_reply_rule(merchant_id, keywords, reply_content, enabled)
      values (?, ?, ?, ?)
      """,
      merchantId, keywords, replyContent, enabled ? 1 : 0);
  return jdbcTemplate.queryForObject(
      "select max(id) from merchant_support_auto_reply_rule where merchant_id = ?",
      Long.class,
      merchantId);
}

void updateAutoReplyRule(long id, String keywords, String replyContent, boolean enabled) {
  jdbcTemplate.update(
      """
      update merchant_support_auto_reply_rule
      set keywords = ?, reply_content = ?, enabled = ?, updated_at = current_timestamp
      where id = ? and is_deleted = 0
      """,
      keywords, replyContent, enabled ? 1 : 0, id);
}

void deleteAutoReplyRule(long id) {
  jdbcTemplate.update(
      "update merchant_support_auto_reply_rule set is_deleted = 1, updated_at = current_timestamp where id = ?",
      id);
}
```

Add mapper and record:

```java
private AutoReplyRuleRow mapAutoReplyRule(ResultSet rs, int rowNum) throws SQLException {
  Timestamp createdAt = rs.getTimestamp("created_at");
  Timestamp updatedAt = rs.getTimestamp("updated_at");
  return new AutoReplyRuleRow(
      rs.getLong("id"),
      rs.getLong("merchant_id"),
      rs.getString("keywords"),
      rs.getString("reply_content"),
      rs.getBoolean("enabled"),
      createdAt == null ? null : createdAt.toLocalDateTime(),
      updatedAt == null ? null : updatedAt.toLocalDateTime());
}

record AutoReplyRuleRow(Long id, Long merchantId, String keywords, String replyContent,
                        boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

- [ ] **Step 6: Add service methods and matching**

In `SupportService.java`, add:

```java
List<SupportAutoReplyRuleView> merchantAutoReplyRules() {
  long merchantId = currentMerchantId();
  return supportRepository.listAutoReplyRules(merchantId).stream()
      .map(this::toAutoReplyRuleView)
      .toList();
}

@Transactional
SupportAutoReplyRuleView createMerchantAutoReplyRule(SupportAutoReplyRuleUpsertRequest request) {
  long merchantId = currentMerchantId();
  Long id = supportRepository.insertAutoReplyRule(
      merchantId,
      cleanRequired(request.keywords(), "触发关键词不能为空"),
      cleanRequired(request.replyContent(), "回复内容不能为空"),
      request.enabled() == null || request.enabled());
  return supportRepository.findAutoReplyRule(id)
      .map(this::toAutoReplyRuleView)
      .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
}

@Transactional
SupportAutoReplyRuleView updateMerchantAutoReplyRule(long ruleId, SupportAutoReplyRuleUpsertRequest request) {
  long merchantId = currentMerchantId();
  SupportRepository.AutoReplyRuleRow row = supportRepository.findAutoReplyRule(ruleId)
      .filter(rule -> rule.merchantId() == merchantId)
      .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  supportRepository.updateAutoReplyRule(
      row.id(),
      cleanRequired(request.keywords(), "触发关键词不能为空"),
      cleanRequired(request.replyContent(), "回复内容不能为空"),
      request.enabled() == null || request.enabled());
  return supportRepository.findAutoReplyRule(ruleId)
      .map(this::toAutoReplyRuleView)
      .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
}

@Transactional
void deleteMerchantAutoReplyRule(long ruleId) {
  long merchantId = currentMerchantId();
  SupportRepository.AutoReplyRuleRow row = supportRepository.findAutoReplyRule(ruleId)
      .filter(rule -> rule.merchantId() == merchantId)
      .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  supportRepository.deleteAutoReplyRule(row.id());
}

private SupportAutoReplyRuleView toAutoReplyRuleView(SupportRepository.AutoReplyRuleRow row) {
  return new SupportAutoReplyRuleView(
      row.id(), row.merchantId(), row.keywords(), row.replyContent(),
      row.enabled(), row.createdAt(), row.updatedAt());
}

private String cleanRequired(String value, String message) {
  String cleaned = value == null ? "" : value.trim();
  if (cleaned.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, message);
  return cleaned;
}
```

Update merchant branch in `autoReplyIfMatched`:

```java
} else {
  for (SupportRepository.AutoReplyRuleRow rule : supportRepository.listEnabledAutoReplyRules(row.merchantId())) {
    if (matchesRule(normalized, rule.keywords())) {
      reply = rule.replyContent();
      break;
    }
  }
}
```

Add:

```java
private boolean matchesRule(String content, String keywords) {
  if (keywords == null || keywords.isBlank()) return false;
  return java.util.Arrays.stream(keywords.split("[,，/、\\s]+"))
      .map(String::trim)
      .filter(keyword -> !keyword.isEmpty())
      .anyMatch(content::contains);
}
```

- [ ] **Step 7: Add controller endpoints**

Add to `SupportMerchantController.java`:

```java
@GetMapping("/auto-reply-rules")
ApiResponse<List<SupportAutoReplyRuleView>> autoReplyRules() {
  return ApiResponse.ok(supportService.merchantAutoReplyRules());
}

@PostMapping("/auto-reply-rules")
ApiResponse<SupportAutoReplyRuleView> createAutoReplyRule(
    @Valid @RequestBody SupportAutoReplyRuleUpsertRequest request) {
  return ApiResponse.ok(supportService.createMerchantAutoReplyRule(request));
}

@PostMapping("/auto-reply-rules/{ruleId}")
ApiResponse<SupportAutoReplyRuleView> updateAutoReplyRule(
    @PathVariable long ruleId,
    @Valid @RequestBody SupportAutoReplyRuleUpsertRequest request) {
  return ApiResponse.ok(supportService.updateMerchantAutoReplyRule(ruleId, request));
}

@PostMapping("/auto-reply-rules/{ruleId}/delete")
ApiResponse<Void> deleteAutoReplyRule(@PathVariable long ruleId) {
  supportService.deleteMerchantAutoReplyRule(ruleId);
  return ApiResponse.ok(null);
}
```

- [ ] **Step 8: Run backend support tests**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/services/backend
mvn -Dtest=SupportServiceTest test
```

Expected: `BUILD SUCCESS`.

---

## Task 2: Merchant Web Editable Auto-Reply Rules

**Files:**
- Modify: `apps/merchant_web/src/types.ts`
- Modify: `apps/merchant_web/src/api.ts`
- Modify: `apps/merchant_web/src/pages/SessionsPage.vue`

- [ ] **Step 1: Add types and API**

Add to `types.ts`:

```ts
export interface SupportAutoReplyRuleView {
  id: number;
  merchantId: number;
  keywords: string;
  replyContent: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}
```

Add to `api.ts`:

```ts
export function fetchAutoReplyRules() {
  return request<SupportAutoReplyRuleView[]>('/api/merchant/ops/sessions/auto-reply-rules');
}

export function createAutoReplyRule(payload: { keywords: string; replyContent: string; enabled: boolean }) {
  return request<SupportAutoReplyRuleView>('/api/merchant/ops/sessions/auto-reply-rules', {
    method: 'POST',
    body: payload,
  });
}

export function updateAutoReplyRule(ruleId: number, payload: { keywords: string; replyContent: string; enabled: boolean }) {
  return request<SupportAutoReplyRuleView>(`/api/merchant/ops/sessions/auto-reply-rules/${ruleId}`, {
    method: 'POST',
    body: payload,
  });
}

export function deleteAutoReplyRule(ruleId: number) {
  return request<void>(`/api/merchant/ops/sessions/auto-reply-rules/${ruleId}/delete`, {
    method: 'POST',
    body: {},
  });
}
```

- [ ] **Step 2: Replace hard-coded rules in `SessionsPage.vue`**

Use refs:

```ts
const autoReplyRules = ref<SupportAutoReplyRuleView[]>([]);
const ruleForm = ref({ id: 0, keywords: '', replyContent: '', enabled: true });
const savingRule = ref(false);
```

Add loader:

```ts
async function loadAutoReplyRules() {
  try {
    autoReplyRules.value = await fetchAutoReplyRules();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}
```

Update `onMounted`:

```ts
await Promise.all([load(), loadTemplates(), loadAutoReplyRules()]);
```

Add save/edit/delete:

```ts
function editRule(rule: SupportAutoReplyRuleView) {
  ruleForm.value = {
    id: rule.id,
    keywords: rule.keywords,
    replyContent: rule.replyContent,
    enabled: rule.enabled,
  };
}

function resetRuleForm() {
  ruleForm.value = { id: 0, keywords: '', replyContent: '', enabled: true };
}

async function saveRule() {
  const keywords = ruleForm.value.keywords.trim();
  const replyContent = ruleForm.value.replyContent.trim();
  if (!keywords || !replyContent) {
    emit('notice', '请填写触发关键词和回复内容');
    return;
  }
  try {
    savingRule.value = true;
    if (ruleForm.value.id > 0) {
      await updateAutoReplyRule(ruleForm.value.id, { keywords, replyContent, enabled: ruleForm.value.enabled });
      emit('notice', '自动回复规则已更新');
    } else {
      await createAutoReplyRule({ keywords, replyContent, enabled: ruleForm.value.enabled });
      emit('notice', '自动回复规则已新增');
    }
    resetRuleForm();
    await loadAutoReplyRules();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    savingRule.value = false;
  }
}

async function removeRule(rule: SupportAutoReplyRuleView) {
  if (!confirm(`删除规则「${rule.keywords}」？`)) return;
  try {
    await deleteAutoReplyRule(rule.id);
    await loadAutoReplyRules();
    emit('notice', '自动回复规则已删除');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}
```

- [ ] **Step 3: Add editable UI**

Replace the current static `auto-rules` block with:

```vue
<div class="auto-rules">
  <div class="rule-head">
    <span class="tip">关键词自动回复</span>
    <button type="button" class="secondary-btn small" @click="resetRuleForm">新建规则</button>
  </div>
  <form class="rule-form" @submit.prevent="saveRule">
    <input v-model="ruleForm.keywords" placeholder="关键词，用逗号分隔，例如：停车,排队" />
    <input v-model="ruleForm.replyContent" placeholder="自动回复内容" />
    <label class="check-line">
      <input v-model="ruleForm.enabled" type="checkbox" />
      启用
    </label>
    <button class="primary-btn small" :disabled="savingRule">{{ savingRule ? '保存中' : '保存规则' }}</button>
  </form>
  <div class="rule-list">
    <div v-for="rule in autoReplyRules" :key="rule.id" class="rule-row">
      <span>{{ rule.keywords }}：{{ rule.replyContent }}</span>
      <small>{{ rule.enabled ? '启用' : '停用' }}</small>
      <button type="button" class="secondary-btn small" @click="editRule(rule)">编辑</button>
      <button type="button" class="secondary-btn small" @click="removeRule(rule)">删除</button>
    </div>
    <div v-if="autoReplyRules.length === 0" class="empty-card">暂无自动回复规则</div>
  </div>
</div>
```

- [ ] **Step 4: Run merchant build**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/merchant_web
npm run build
```

Expected: `✓ built`.

---

## Task 3: User Chat Distinguishes Merchant and Platform Messages

**Files:**
- Modify: `apps/user_app/lib/features/support/data/support_repository.dart`
- Modify: `apps/user_app/lib/features/support/presentation/support_chat_page.dart`

- [ ] **Step 1: Add message helper**

Add to `SupportMessage`:

```dart
bool get isMerchant => senderType == 'merchant';
```

- [ ] **Step 2: Update `_MessageBubble`**

Replace the style calculation in `support_chat_page.dart`:

```dart
final isUser = message.isUser;
final align = isUser ? Alignment.centerRight : Alignment.centerLeft;
final bgColor = isUser
    ? Colors.blue.shade50
    : message.isPlatform
        ? Colors.orange.shade50
        : Colors.grey.shade200;
final label = isUser
    ? '我'
    : message.isPlatform
        ? '平台客服'
        : '商家客服';
```

Replace the child content with:

```dart
child: Column(
  crossAxisAlignment: CrossAxisAlignment.start,
  children: [
    Text(
      label,
      style: TextStyle(
        fontSize: 11,
        color: isUser ? Colors.blueGrey : Colors.grey.shade700,
        fontWeight: FontWeight.w600,
      ),
    ),
    const SizedBox(height: 3),
    Text(message.content),
  ],
),
```

- [ ] **Step 3: Run Flutter checks**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/user_app
flutter analyze
flutter test
```

Expected: `No issues found` and `All tests passed`.

---

## Task 4: Merchant Complaint Ticket Page and Correct Entry

**Files:**
- Modify: `services/backend/src/main/java/com/aituan/complaint/ComplaintRepository.java`
- Modify: `services/backend/src/main/java/com/aituan/complaint/ComplaintService.java`
- Create: `services/backend/src/main/java/com/aituan/complaint/ComplaintMerchantController.java`
- Modify: `services/backend/src/test/java/com/aituan/complaint/ComplaintServiceTest.java`
- Modify: `apps/merchant_web/src/types.ts`
- Modify: `apps/merchant_web/src/api.ts`
- Create: `apps/merchant_web/src/pages/ComplaintsPage.vue`
- Modify: `apps/merchant_web/src/App.vue`
- Modify: `apps/merchant_web/src/components/ConsoleFrame.vue`
- Modify: `apps/merchant_web/src/pages/SessionsPage.vue`

- [ ] **Step 1: Write backend tests**

Add to `ComplaintServiceTest`:

```java
@Test
void merchantCanListOnlyOwnComplaintTickets() {
  TestAuthSupport.loginAsMerchant(30L);

  var page = complaintService.merchantTickets(null, null, null, 1, 20);

  assertThat(page.list()).allSatisfy(ticket -> assertThat(ticket.merchantId()).isEqualTo(11L));
}

@Test
void merchantCannotOpenOtherMerchantComplaintTicket() {
  TestAuthSupport.loginAsMerchant(30L);

  assertThatThrownBy(() -> complaintService.merchantTicketDetail(1L))
      .isInstanceOf(BusinessException.class)
      .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
          .isEqualTo(ErrorCode.NOT_FOUND));
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/services/backend
mvn -Dtest=ComplaintServiceTest test
```

Expected: compile failure because merchant complaint methods do not exist.

- [ ] **Step 3: Add merchant complaint backend**

Add repository methods:

```java
long countMerchantTickets(long merchantId, String status, String orderNo, String storeName) {
  // Same filter shape as admin, plus `t.merchant_id = ?`.
}

List<TicketRow> listMerchantTickets(long merchantId, String status, String orderNo, String storeName, int offset, int limit) {
  // Same select as admin, plus `t.merchant_id = ?`, sorted by created_at desc.
}
```

Add service methods:

```java
PageResponse<ComplaintView> merchantTickets(String status, String orderNo, String storeName, int page, int pageSize) {
  long merchantId = currentMerchantId();
  String normalized = normalizeStatus(status);
  long total = complaintRepository.countMerchantTickets(merchantId, normalized, orderNo, storeName);
  List<ComplaintView> list = complaintRepository
      .listMerchantTickets(merchantId, normalized, orderNo, storeName, (page - 1) * pageSize, pageSize)
      .stream()
      .map(row -> toView(row, true))
      .toList();
  return PageResponse.of(list, page, pageSize, total);
}

ComplaintDetailView merchantTicketDetail(long id) {
  long merchantId = currentMerchantId();
  ComplaintRepository.TicketRow row = complaintRepository.findById(id)
      .filter(ticket -> ticket.merchantId() != null && ticket.merchantId() == merchantId)
      .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  List<ComplaintLogView> logs = complaintRepository.listLogs(id).stream().map(this::toLogView).toList();
  return new ComplaintDetailView(toView(row, true), logs);
}
```

Create controller:

```java
@RestController
@RequestMapping("/api/merchant/ops/complaints")
@Validated
class ComplaintMerchantController {
  private final ComplaintService complaintService;

  ComplaintMerchantController(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  @GetMapping
  ApiResponse<PageResponse<ComplaintView>> tickets(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String orderNo,
      @RequestParam(required = false) String storeName,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(complaintService.merchantTickets(status, orderNo, storeName, page, pageSize));
  }

  @GetMapping("/{id}")
  ApiResponse<ComplaintDetailView> detail(@PathVariable long id) {
    return ApiResponse.ok(complaintService.merchantTicketDetail(id));
  }
}
```

- [ ] **Step 4: Add merchant web complaint route**

In `types.ts`, add:

```ts
| 'complaints'
```

to `ConsolePage`, and add:

```ts
export interface ComplaintView {
  id: number;
  ticketNo: string;
  orderId?: number;
  orderNo?: string;
  storeId?: number;
  merchantId?: number;
  storeName?: string;
  category: string;
  title: string;
  detail: string;
  evidenceUrls?: string[];
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ComplaintDetailView {
  complaint: ComplaintView;
  logs: Array<{ id: number; action: string; operatorType: string; remark?: string; createdAt: string }>;
}
```

In `api.ts`, add:

```ts
export function fetchMerchantComplaints(params: { status?: string; orderNo?: string; storeName?: string; page?: number; pageSize?: number } = {}) {
  const query = new URLSearchParams({
    page: String(params.page || 1),
    pageSize: String(params.pageSize || 20),
  });
  if (params.status) query.set('status', params.status);
  if (params.orderNo) query.set('orderNo', params.orderNo);
  if (params.storeName) query.set('storeName', params.storeName);
  return request<PageResponse<ComplaintView>>(`/api/merchant/ops/complaints?${query}`);
}

export function fetchMerchantComplaintDetail(id: number) {
  return request<ComplaintDetailView>(`/api/merchant/ops/complaints/${id}`);
}
```

- [ ] **Step 5: Create `ComplaintsPage.vue`**

Implement list/detail with filters `orderNo` and `storeName`. Use the same panel/table pattern as `ReviewPage.vue` and `SessionsPage.vue`, but do not include admin actions like accept/resolve/close unless explicitly required.

Essential script state:

```ts
const complaints = ref<ComplaintView[]>([]);
const active = ref<ComplaintDetailView | null>(null);
const orderNo = ref('');
const storeName = ref('');
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const page = await fetchMerchantComplaints({
      orderNo: orderNo.value.trim(),
      storeName: storeName.value.trim(),
      pageSize: 50,
    });
    complaints.value = page.list;
  } finally {
    loading.value = false;
  }
}

async function openComplaint(row: ComplaintView) {
  active.value = await fetchMerchantComplaintDetail(row.id);
}
```

- [ ] **Step 6: Wire merchant console route**

In `App.vue`, import `ComplaintsPage`, add title:

```ts
complaints: '投诉工单',
```

Add component:

```vue
<ComplaintsPage v-else-if="activePage === 'complaints'" :refresh-key="refreshKey" @notice="setNotice" />
```

In `ConsoleFrame.vue`, add nav entry for `complaints`.

- [ ] **Step 7: Fix `SessionsPage.vue` complaint entry**

Replace `window.open` admin URL logic with:

```ts
const emit = defineEmits<{
  notice: [message: string];
  openComplaints: [filters: { orderNo?: string; storeName?: string }];
}>();

function openComplaintEntry() {
  emit('openComplaints', {
    orderNo: activeSession.value?.relatedOrderNo,
    storeName: activeSession.value?.storeName,
  });
}
```

In `App.vue`, handle:

```vue
<SessionsPage
  v-else-if="activePage === 'sessions'"
  :refresh-key="refreshKey"
  @notice="setNotice"
  @open-complaints="activePage = 'complaints'"
/>
```

If preserving filters is needed, add `complaintFilters` state in `App.vue` and pass it into `ComplaintsPage`.

- [ ] **Step 8: Run backend and merchant web checks**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/services/backend
mvn -Dtest=ComplaintServiceTest test

cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/merchant_web
npm run build
```

Expected: backend `BUILD SUCCESS`, merchant web `✓ built`.

---

## Task 5: Regression Script and Full Verification

**Files:**
- Modify: `scripts/verify/member_e_regression_checks.sh`

- [ ] **Step 1: Add static regression checks**

Append checks:

```bash
if ! grep -q "auto-reply-rules" "$ROOT/apps/merchant_web/src/api.ts"; then
  fail "merchant web should expose editable auto reply rules API"
fi

if ! grep -q "SupportAutoReplyRuleView" "$ROOT/apps/merchant_web/src/pages/SessionsPage.vue"; then
  fail "merchant sessions should render editable auto reply rules"
fi

if ! grep -q "message.isPlatform" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "user support chat should visually distinguish platform messages"
fi

if ! grep -q "message.isMerchant" "$ROOT/apps/user_app/lib/features/support/presentation/support_chat_page.dart"; then
  fail "user support chat should visually distinguish merchant messages"
fi

if ! grep -q "ComplaintsPage" "$ROOT/apps/merchant_web/src/App.vue"; then
  fail "merchant web should route to its own complaint ticket page"
fi

if grep -q "hostname}:5174" "$ROOT/apps/merchant_web/src/pages/SessionsPage.vue"; then
  fail "merchant complaint entry should not open the admin web port"
fi
```

- [ ] **Step 2: Run full verification**

Run:

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan/services/backend
mvn test

cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/user_app
flutter analyze
flutter test

cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/merchant_web
npm run build

cd /Users/camellia/Desktop/puaa/aituan/aituan/apps/admin_web
npm run build

cd /Users/camellia/Desktop/puaa/aituan/aituan
bash scripts/verify/member_e_regression_checks.sh
git diff --check
```

Expected:
- Maven: `BUILD SUCCESS`
- Flutter analyze: `No issues found`
- Flutter test: `All tests passed`
- Both Vite builds: `✓ built`
- Regression script: `member E regression checks passed`
- `git diff --check`: no output

- [ ] **Step 3: Commit and push**

```bash
cd /Users/camellia/Desktop/puaa/aituan/aituan
git status --short
git add database/migrations/V013__merchant_support_auto_reply_rules.sql \
  services/backend/src/main/java/com/aituan/support/SupportDtos.java \
  services/backend/src/main/java/com/aituan/support/SupportRepository.java \
  services/backend/src/main/java/com/aituan/support/SupportService.java \
  services/backend/src/main/java/com/aituan/support/SupportMerchantController.java \
  services/backend/src/test/java/com/aituan/support/SupportServiceTest.java \
  services/backend/src/main/java/com/aituan/complaint/ComplaintRepository.java \
  services/backend/src/main/java/com/aituan/complaint/ComplaintService.java \
  services/backend/src/main/java/com/aituan/complaint/ComplaintMerchantController.java \
  services/backend/src/test/java/com/aituan/complaint/ComplaintServiceTest.java \
  apps/merchant_web/src/types.ts \
  apps/merchant_web/src/api.ts \
  apps/merchant_web/src/pages/SessionsPage.vue \
  apps/merchant_web/src/pages/ComplaintsPage.vue \
  apps/merchant_web/src/App.vue \
  apps/merchant_web/src/components/ConsoleFrame.vue \
  apps/user_app/lib/features/support/data/support_repository.dart \
  apps/user_app/lib/features/support/presentation/support_chat_page.dart \
  scripts/verify/member_e_regression_checks.sh
git commit -m "Fix support auto replies and merchant complaint flow"
GIT_TERMINAL_PROMPT=0 git -c http.proxy=http://127.0.0.1:7892 -c https.proxy=http://127.0.0.1:7892 push origin member-e-improvements
```

---

## Self-Review

- Spec coverage:
  - 商家不能自己修改自动回复触发条件和回复内容：Task 1 and Task 2 add backend storage, matching, and editable merchant UI.
  - 用户端申请平台客服介入后无法区分商家和平台客服消息：Task 3 styles `merchant` and `platform` message sources separately.
  - 商家端投诉工单点进去显示不正确页面：Task 4 adds merchant complaint endpoints/page and removes admin-web jump.
- Placeholder scan:
  - No deferred backend/API method names. `ComplaintsPage.vue` UI layout is intentionally summarized but has concrete state and behavior.
- Type consistency:
  - DTO names match across backend, frontend API, and types.
  - Existing endpoint prefix `/api/merchant/ops/sessions` is preserved for support rules.
