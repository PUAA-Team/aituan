package com.aituan.engagementplatform.complaint;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/app/complaints")
@Validated
class ComplaintController {
  private final ComplaintService complaintService;

  ComplaintController(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  @GetMapping
  ApiResponse<PageResponse<ComplaintView>> tickets(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(complaintService.myTickets(status, page, pageSize));
  }

  @PostMapping
  ApiResponse<ComplaintView> submit(@Valid @RequestBody ComplaintCreateRequest request) {
    return ApiResponse.ok(complaintService.submit(request));
  }

  @GetMapping("/{id}")
  ApiResponse<ComplaintDetailView> detail(@PathVariable long id) {
    return ApiResponse.ok(complaintService.myTicketDetail(id));
  }

  @PostMapping("/{id}/supplements")
  ApiResponse<ComplaintDetailView> supplement(@PathVariable long id, @Valid @RequestBody ComplaintSupplementRequest request) {
    return ApiResponse.ok(complaintService.supplement(id, request));
  }
}

