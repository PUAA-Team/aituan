package com.aituan.engagementplatform.complaint;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/governance/complaints")
@Validated
class ComplaintAdminController {
  private final ComplaintService complaintService;

  ComplaintAdminController(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  @GetMapping
  ApiResponse<PageResponse<ComplaintView>> tickets(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String orderNo,
      @RequestParam(required = false) String storeName,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(complaintService.adminTickets(status, category, orderNo, storeName, page, pageSize));
  }

  @PostMapping("/{id}/accept")
  ApiResponse<ComplaintView> accept(@PathVariable long id, @RequestBody(required = false) ComplaintActionRequest request) {
    return ApiResponse.ok(complaintService.accept(id, request));
  }

  @PostMapping("/{id}/resolve")
  ApiResponse<ComplaintView> resolve(@PathVariable long id, @RequestBody(required = false) ComplaintActionRequest request) {
    return ApiResponse.ok(complaintService.resolve(id, request));
  }

  @PostMapping("/{id}/close")
  ApiResponse<ComplaintView> close(@PathVariable long id, @RequestBody(required = false) ComplaintActionRequest request) {
    return ApiResponse.ok(complaintService.close(id, request));
  }
}

