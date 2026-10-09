package com.todolist.shared.web;

import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Uniform JSON error responses for REST controllers.
 *
 * <p>Scoped to {@link RestController} beans and ordered ahead of the view advice. Failures raised
 * before a handler method is resolved — an unknown path, an unsupported method — never reach an
 * annotation scoped advice, so {@link com.todolist.shared.exception.GlobalExceptionHandler}
 * answers those and decides JSON or HTML from the request path.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final String MESSAGE_REQUIRED = "必須項目です。";
  private static final String MESSAGE_MALFORMED = "値の形式が正しくありません。";

  /**
   * @param exception bean validation failure on a request body
   * @param request failing request
   * @return 400 with a field name to message map
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ApiErrors.response(AppErrorCode.INVALID_REQUEST,
        AppErrorCode.INVALID_REQUEST.getDefaultMessage(), request, fieldErrors);
  }

  /**
   * @param exception bean validation failure on a method parameter
   * @param request failing request
   * @return 400 describing the rejected request
   */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleParameterValidation(
      HandlerMethodValidationException exception, HttpServletRequest request) {
    return ApiErrors.response(AppErrorCode.INVALID_REQUEST, request);
  }

  /**
   * Covers an unparseable path variable or query parameter, including a value that is not one of
   * an enum's constants.
   *
   * @param exception conversion failure on a path variable or query parameter
   * @param request failing request
   * @return 400 naming the parameter, and the accepted values when the target is an enum
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    Class<?> requiredType = exception.getRequiredType();
    String detail = MESSAGE_MALFORMED;
    if (requiredType != null && requiredType.isEnum()) {
      detail = "指定できる値: " + Arrays.stream(requiredType.getEnumConstants())
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }
    return ApiErrors.response(AppErrorCode.INVALID_REQUEST,
        AppErrorCode.INVALID_REQUEST.getDefaultMessage(), request,
        Map.of(exception.getName(), detail));
  }

  /**
   * @param exception a required query parameter was absent
   * @param request failing request
   * @return 400 naming the missing parameter
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return ApiErrors.response(AppErrorCode.INVALID_REQUEST,
        AppErrorCode.INVALID_REQUEST.getDefaultMessage(), request,
        Map.of(exception.getParameterName(), MESSAGE_REQUIRED));
  }

  /**
   * @param exception unparseable or type mismatched request body
   * @param request failing request
   * @return 400 describing the rejected request
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    log.debug("リクエストボディを解析できません: path={}", request.getRequestURI());
    return ApiErrors.response(AppErrorCode.INVALID_REQUEST, "リクエストの形式が正しくありません。",
        request, Map.of());
  }

  /**
   * Reached when a handler method matched but could not consume the body's media type. The
   * annotation scoped advice takes precedence in that case, so it must cover this itself.
   *
   * @param exception request body media type not supported
   * @param request failing request
   * @return 415 describing the rejected content type
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
    log.debug("未対応の形式: path={}", request.getRequestURI());
    return ApiErrors.response(AppErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
  }

  /**
   * @param exception application exception carrying a user facing error code
   * @param request failing request
   * @return response using the status mapped to the error code
   */
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiErrorResponse> handleAppException(AppException exception,
      HttpServletRequest request) {
    AppErrorCode errorCode = exception.getErrorCode();
    log.warn("業務エラー: code={}, path={}", errorCode.getCode(), request.getRequestURI());
    return ApiErrors.response(errorCode, exception.getMessage(), request, Map.of());
  }

  /**
   * @param exception failed authentication raised by Spring Security
   * @param request failing request
   * @return 401 with no detail about which credential was wrong
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception,
      HttpServletRequest request) {
    log.warn("認証失敗: path={}, reason={}", request.getRequestURI(),
        exception.getClass().getSimpleName());
    return ApiErrors.response(AppErrorCode.INVALID_CREDENTIALS, request);
  }

  /**
   * @param exception authorisation failure raised by Spring Security
   * @param request failing request
   * @return 403 describing the denied access
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception,
      HttpServletRequest request) {
    log.warn("アクセス拒否: path={}", request.getRequestURI());
    return ApiErrors.response(AppErrorCode.ACCESS_DENIED, request);
  }

  /**
   * @param exception failure that is not attributable to the caller
   * @param request failing request
   * @return 500 that leaks no internal detail
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception,
      HttpServletRequest request) {
    log.error("想定外エラー: path={}", request.getRequestURI(), exception);
    return ApiErrors.response(AppErrorCode.INTERNAL_ERROR, request);
  }
}
