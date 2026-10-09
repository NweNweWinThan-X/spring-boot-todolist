package com.todolist.shared.web;

import com.todolist.shared.exception.AppErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Builds the uniform error body.
 *
 * <p>Shared by the REST advice, the view advice and the security filters so that every failure on
 * an API path answers with the same shape.
 */
public final class ApiErrors {

  private static final String API_PREFIX = "/api/";

  private ApiErrors() {
  }

  /**
   * @param request request being handled
   * @return whether the caller expects JSON rather than a rendered page
   */
  public static boolean isApiRequest(HttpServletRequest request) {
    return request.getRequestURI().startsWith(API_PREFIX);
  }

  /**
   * @param errorCode error classification, also drives the HTTP status
   * @param message user facing message, free of internal detail
   * @param request request being handled
   * @param fieldErrors field name to validation message, empty when not a validation failure
   * @return error body for the given classification
   */
  public static ApiErrorResponse body(AppErrorCode errorCode, String message,
      HttpServletRequest request, Map<String, String> fieldErrors) {
    return new ApiErrorResponse(
        Instant.now(),
        errorCode.getStatus().value(),
        errorCode.getCode(),
        message,
        request.getRequestURI(),
        fieldErrors
    );
  }

  /**
   * @param errorCode error classification, also drives the HTTP status
   * @param message user facing message, free of internal detail
   * @param request request being handled
   * @param fieldErrors field name to validation message, empty when not a validation failure
   * @return response carrying the error body and the mapped status
   */
  public static ResponseEntity<ApiErrorResponse> response(AppErrorCode errorCode, String message,
      HttpServletRequest request, Map<String, String> fieldErrors) {
    HttpStatus status = errorCode.getStatus();
    return ResponseEntity.status(status).body(body(errorCode, message, request, fieldErrors));
  }

  /**
   * @param errorCode error classification, also drives the HTTP status
   * @param request request being handled
   * @return response using the error code's own message and no field errors
   */
  public static ResponseEntity<ApiErrorResponse> response(AppErrorCode errorCode,
      HttpServletRequest request) {
    return response(errorCode, errorCode.getDefaultMessage(), request, Map.of());
  }
}
