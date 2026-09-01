package com.aituan.engagementplatform.platform;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Validated
class PlatformAdminController {
  private final PlatformService service;
  PlatformAdminController(PlatformService service){this.service=service;}
  @GetMapping("/announcements") ApiResponse<PageResponse<AnnouncementView>> announcements(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")@Min(1)int page,@RequestParam(defaultValue="20")@Min(1)int pageSize){return ApiResponse.ok(service.announcements(status,page,pageSize));}
  @PostMapping("/announcements") ApiResponse<AnnouncementView> create(@Valid @RequestBody AnnouncementUpsertRequest r){return ApiResponse.ok(service.create(r));}
  @PutMapping("/announcements/{id}") ApiResponse<AnnouncementView> update(@PathVariable long id,@Valid @RequestBody AnnouncementUpsertRequest r){return ApiResponse.ok(service.update(id,r));}
  @PostMapping("/announcements/{id}/status") ApiResponse<AnnouncementView> status(@PathVariable long id,@Valid @RequestBody StatusRequest r){return ApiResponse.ok(service.status(id,r));}
  @GetMapping("/audit-logs") ApiResponse<PageResponse<AuditLogView>> audits(@RequestParam(required=false)String actionType,@RequestParam(defaultValue="1")@Min(1)int page,@RequestParam(defaultValue="20")@Min(1)int pageSize){return ApiResponse.ok(service.audits(actionType,page,pageSize));}
  @GetMapping("/configs") ApiResponse<List<ConfigView>> configs(){return ApiResponse.ok(service.configs());}
  @PutMapping("/configs/{key}") ApiResponse<List<ConfigView>> config(@PathVariable String key,@Valid @RequestBody ConfigUpdateRequest r){return ApiResponse.ok(service.updateConfig(key,r));}
  @GetMapping("/dashboard") ApiResponse<DashboardView> dashboard(){return ApiResponse.ok(service.dashboard());}
  @GetMapping("/delivery/settings") ApiResponse<DeliverySettingView> delivery(){return ApiResponse.ok(service.deliverySettings());}
  @PostMapping("/delivery/settings") ApiResponse<DeliverySettingView> delivery(@RequestBody DeliverySettingRequest r){return ApiResponse.ok(service.updateDelivery(r));}
  @GetMapping("/governance/dashboard") ApiResponse<Map<String,Long>> governance(){return ApiResponse.ok(service.governance());}
}
