package com.aituan.common.exception;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleBusiness(BusinessException exception) {
    return ApiResponse.fail(exception.getErrorCode().code(), exception.getMessage());
  }

  @ExceptionHandler({BadCredentialsException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResponse<Void> handleUnauthorized(Exception exception) {
    return ApiResponse.fail(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message());
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> handleForbidden(AccessDeniedException exception) {
    return ApiResponse.fail(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining("；"));
    return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleConstraint(ConstraintViolationException exception) {
    return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
    return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), "图片大小不能超过 5MB");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleUnexpected(Exception exception) {
    log.error("Unhandled request exception", exception);
    return ApiResponse.fail(9999, "服务暂时不可用");
  }
}
