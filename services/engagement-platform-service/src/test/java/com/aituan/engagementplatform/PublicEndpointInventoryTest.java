package com.aituan.engagementplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class PublicEndpointInventoryTest {
  @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

  @Test
  void exposesExactlyTheFiftyNineOperationsOwnedByMemberD() {
    Set<String> actual = new HashSet<>();
    mappings.getHandlerMethods().forEach((mapping, handler) -> {
      if (!handler.getBeanType().getPackageName().startsWith("com.aituan.engagementplatform")) return;
      Set<String> paths = mapping.getPatternValues();
      mapping.getMethodsCondition().getMethods().forEach(method ->
          paths.stream().filter(path -> path.startsWith("/api/")).forEach(path ->
              actual.add(method.name() + " " + path)));
    });

    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected());
    assertThat(actual).hasSize(59);
  }

  private Set<String> expected() {
    return Set.of(
        "GET /api/admin/announcements",
        "POST /api/admin/announcements",
        "PUT /api/admin/announcements/{id}",
        "POST /api/admin/announcements/{id}/status",
        "GET /api/admin/audit-logs",
        "GET /api/admin/configs",
        "PUT /api/admin/configs/{key}",
        "GET /api/admin/dashboard",
        "GET /api/admin/delivery/settings",
        "POST /api/admin/delivery/settings",
        "GET /api/admin/governance/complaints",
        "POST /api/admin/governance/complaints/{id}/accept",
        "POST /api/admin/governance/complaints/{id}/close",
        "POST /api/admin/governance/complaints/{id}/resolve",
        "GET /api/admin/governance/dashboard",
        "GET /api/admin/governance/reviews",
        "POST /api/admin/governance/reviews/{id}/audit",
        "GET /api/admin/governance/support/sessions",
        "GET /api/admin/governance/support/sessions/{id}",
        "POST /api/admin/governance/support/sessions/{id}/messages",
        "GET /api/app/ai/assistant/conversations/{conversationId}/messages",
        "GET /api/app/ai/assistant/conversations/current",
        "POST /api/app/ai/assistant/message",
        "GET /api/app/complaints",
        "POST /api/app/complaints",
        "GET /api/app/complaints/{id}",
        "POST /api/app/complaints/{id}/supplements",
        "GET /api/app/interaction/orders/{orderId}/review",
        "POST /api/app/interaction/orders/{orderId}/review",
        "GET /api/app/interaction/reviews/{id}",
        "POST /api/app/interaction/reviews/{id}/helpful",
        "POST /api/app/interaction/reviews/{id}/report",
        "GET /api/app/interaction/reviews/me",
        "GET /api/app/interaction/stores/{storeId}/reviews",
        "GET /api/app/support/sessions",
        "POST /api/app/support/sessions",
        "GET /api/app/support/sessions/{id}",
        "POST /api/app/support/sessions/{id}/close",
        "POST /api/app/support/sessions/{id}/handoff",
        "POST /api/app/support/sessions/{id}/messages",
        "POST /api/app/support/sessions/{id}/platform-intervention",
        "GET /api/common/demo-images/{fileName}",
        "GET /api/common/files/{bizType}/{fileName}",
        "POST /api/common/files/upload",
        "GET /api/merchant/ops/complaints",
        "GET /api/merchant/ops/complaints/{id}",
        "GET /api/merchant/ops/reviews",
        "GET /api/merchant/ops/reviews/{id}",
        "POST /api/merchant/ops/reviews/{id}/reply",
        "GET /api/merchant/ops/sessions",
        "GET /api/merchant/ops/sessions/{id}",
        "POST /api/merchant/ops/sessions/{id}/close",
        "POST /api/merchant/ops/sessions/{id}/messages",
        "POST /api/merchant/ops/sessions/{id}/platform-intervention",
        "GET /api/merchant/ops/sessions/auto-reply-rules",
        "POST /api/merchant/ops/sessions/auto-reply-rules",
        "POST /api/merchant/ops/sessions/auto-reply-rules/{ruleId}",
        "POST /api/merchant/ops/sessions/auto-reply-rules/{ruleId}/delete",
        "GET /api/merchant/ops/sessions/templates");
  }
}
