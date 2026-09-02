package com.aituan.tradefulfillment.trade.api;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import com.aituan.tradefulfillment.trade.TradeService;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.AdminDeliveryTaskView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.DeliveryActionRequest;
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
@RequestMapping("/api/admin/delivery/tasks")
@Validated
public class AdminDeliveryController {
  private final TradeService tradeService;

  public AdminDeliveryController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AdminDeliveryTaskView>> list(
      @RequestParam(required = false) String stage,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.deliveryTasks(stage, page, pageSize));
  }

  @GetMapping("/{taskId}")
  public ApiResponse<AdminDeliveryTaskView> detail(@PathVariable long taskId) {
    return ApiResponse.ok(tradeService.deliveryTask(taskId));
  }

  @PostMapping("/{taskId}/advance")
  public ApiResponse<AdminDeliveryTaskView> advance(@PathVariable long taskId, @RequestBody(required = false) DeliveryActionRequest request) {
    return ApiResponse.ok(tradeService.advanceDeliveryTask(taskId, request));
  }

  @PostMapping("/{taskId}/pause")
  public ApiResponse<AdminDeliveryTaskView> pause(@PathVariable long taskId) {
    return ApiResponse.ok(tradeService.pauseDeliveryTask(taskId));
  }

  @PostMapping("/{taskId}/resume")
  public ApiResponse<AdminDeliveryTaskView> resume(@PathVariable long taskId) {
    return ApiResponse.ok(tradeService.resumeDeliveryTask(taskId));
  }

  @PostMapping("/{taskId}/abnormal")
  public ApiResponse<AdminDeliveryTaskView> abnormal(@PathVariable long taskId, @RequestBody(required = false) DeliveryActionRequest request) {
    return ApiResponse.ok(tradeService.markDeliveryAbnormal(taskId, request));
  }
}
