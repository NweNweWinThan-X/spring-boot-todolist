package com.todolist.shared.exception;

import com.todolist.shared.web.ApiErrors;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Last-resort advice.
 *
 * <p>Runs at lowest precedence, so anything raised inside a REST controller is handled by
 * {@link com.todolist.shared.web.ApiExceptionHandler} first. What reaches here is either a
 * view request, or a failure raised before a handler method was resolved — an unknown path, an
 * unsupported method, an unsupported content type. Those carry no handler for an annotation
 * scoped advice to match on, so the response format is chosen from the request path instead:
 * JSON under {@code /api/}, an error page elsewhere.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * @param exception application exception carrying a user facing error code
   * @param request failing request
   * @return the JSON error body
   */
  @ExceptionHandler(AppException.class)
  public Object handleAppException(AppException exception, HttpServletRequest request) {
    AppErrorCode errorCode = exception.getErrorCode();
    log.warn("業務エラー: code={}", errorCode.getCode(), exception);
    return render(errorCode, exception.getMessage(), request);
  }

  /**
   * @param exception authorisation failure raised by Spring Security
   * @param request failing request
   * @return the JSON error body
   */
  @ExceptionHandler(AccessDeniedException.class)
  public Object handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
    log.warn("アクセス拒否: code={}", AppErrorCode.ACCESS_DENIED.getCode(), exception);
    return render(AppErrorCode.ACCESS_DENIED, AppErrorCode.ACCESS_DENIED.getDefaultMessage(),
        request);
  }

  /**
   * @param exception no handler or static resource matched the path
   * @param request failing request
   * @return 404 JSON error body
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public Object handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
    log.debug("未定義のパス: path={}", request.getRequestURI());
    return render(AppErrorCode.RESOURCE_NOT_FOUND,
        AppErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(), request);
  }

  /**
   * @param exception HTTP method not supported on the matched path
   * @param request failing request
   * @return 405 JSON error body
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request) {
    log.debug("未対応のメソッド: method={}, path={}", exception.getMethod(),
        request.getRequestURI());
    return render(AppErrorCode.METHOD_NOT_ALLOWED,
        AppErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage(), request);
  }

  /**
   * @param exception request body media type not supported
   * @param request failing request
   * @return 415 JSON error body
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public Object handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception,
      HttpServletRequest request) {
    log.debug("未対応の形式: path={}", request.getRequestURI());
    return render(AppErrorCode.UNSUPPORTED_MEDIA_TYPE,
        AppErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(), request);
  }

  /**
   * @param exception failure that is not attributable to the caller
   * @param request failing request
   * @return generic JSON error body
   */
  @ExceptionHandler(Exception.class)
  public Object handleUnexpected(Exception exception, HttpServletRequest request) {
    log.error("想定外エラー: code={}", AppErrorCode.INTERNAL_ERROR.getCode(), exception);
    return render(AppErrorCode.INTERNAL_ERROR, AppErrorCode.INTERNAL_ERROR.getDefaultMessage(),
        request);
  }

  private Object render(AppErrorCode errorCode, String message, HttpServletRequest request) {
    return ApiErrors.response(errorCode, message, request, Map.of());
  }
}
