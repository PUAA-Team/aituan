package com.aituan.tradefulfillment.trade.api;

import com.aituan.common.api.ApiResponse;
import com.aituan.tradefulfillment.trade.TradeService;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemQuantityRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PaymentMethodView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/trade")
@Validated
public class TradeController {
  private final TradeService tradeService;

  public TradeController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/payment-methods")
  public ApiResponse<List<PaymentMethodView>> paymentMethods() {
    return ApiResponse.ok(tradeService.paymentMethods());
  }

  @GetMapping("/cart")
  public ApiResponse<CartView> cart(@RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.getCart(storeId));
  }

  @PostMapping("/cart/items")
  public ApiResponse<CartView> addCartItem(@Valid @RequestBody CartItemRequest request) {
    return ApiResponse.ok(tradeService.addCartItem(request));
  }

  @PutMapping("/cart/items/{itemId}")
  public ApiResponse<CartView> updateCartItem(@PathVariable long itemId, @Valid @RequestBody CartItemQuantityRequest request) {
    return ApiResponse.ok(tradeService.updateCartItem(itemId, request));
  }

  @DeleteMapping("/cart/items/{itemId}")
  public ApiResponse<CartView> removeCartItem(@PathVariable long itemId, @RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.removeCartItem(storeId, itemId));
  }

  @DeleteMapping("/cart")
  public ApiResponse<CartView> clearCart(@RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.clearCart(storeId));
  }

  @PostMapping("/checkout/preview")
  public ApiResponse<CheckoutPreviewView> preview(@Valid @RequestBody CheckoutPreviewRequest request) {
    return ApiResponse.ok(tradeService.preview(request));
  }
}
