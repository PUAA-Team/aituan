package com.aituan.engagementplatform.platform;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
class PlatformInternalController {
  private final PlatformService service;
  PlatformInternalController(PlatformService service){this.service=service;}
  @GetMapping("/reviews/stores/{storeId}/summary") ApiResponse<ReviewSummaryView> summary(@PathVariable long storeId){return ApiResponse.ok(service.reviewSummary(storeId));}
  @GetMapping("/metrics/stores/{storeId}/engagement") ApiResponse<StoreEngagementView> engagement(@PathVariable long storeId){return ApiResponse.ok(service.storeEngagement(storeId));}
  @GetMapping("/metrics/platform/governance") ApiResponse<Map<String,Long>> governance(){return ApiResponse.ok(service.governance());}
  @PostMapping("/audit-logs") ApiResponse<InternalAuditLogView> audit(@RequestHeader("X-Caller-Service")String caller,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody InternalAuditLogRequest r){return ApiResponse.ok(service.audit(caller,key,r));}
}
