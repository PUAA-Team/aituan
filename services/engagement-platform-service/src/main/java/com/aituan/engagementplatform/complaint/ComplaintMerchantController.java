package com.aituan.engagementplatform.complaint;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

