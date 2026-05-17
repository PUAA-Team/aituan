package com.aituan.message;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/message")
@Validated
class MessageController {
  private final MessageService messageService;

  MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/station")
  ApiResponse<PageResponse<MessageView>> listMessages(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(messageService.listMessages(page, pageSize));
  }

  @PatchMapping("/station/{messageId}/read")
  ApiResponse<Void> markRead(@PathVariable long messageId) {
    messageService.markRead(messageId);
    return ApiResponse.ok(null);
  }
}
