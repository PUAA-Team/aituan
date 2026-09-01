package com.aituan.common.exception;

public enum ErrorCode {
  BAD_REQUEST(1000, "请求参数错误"),
  UNAUTHORIZED(2001, "未登录或登录已过期"),
  FORBIDDEN(2002, "无权限访问"),
  NOT_FOUND(3001, "资源不存在"),
  INVALID_ACCOUNT(3002, "账号格式不正确"),
  INVALID_PASSWORD(3003, "密码错误"),
  VERIFICATION_CODE_INVALID(3004, "验证码无效或已过期"),
  BUSINESS_RULE_VIOLATION(5001, "业务状态不允许"),
  ORDER_STATE_INVALID(5002, "订单状态不允许该操作"),
  ITEM_STOCK_NOT_ENOUGH(5003, "库存不足"),
  DUPLICATE_SUBMISSION(5004, "请勿重复提交"),
  INTERNAL_ERROR(9999, "服务暂时不可用");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }
}
