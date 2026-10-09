package com.todolist.shared.exception;

/**
 * Single application exception type.
 *
 * <p>{@code IllegalArgumentException}, {@code RuntimeException} and bare {@code Exception} must not
 * be thrown from application code — throw this with a {@link AppErrorCode} instead.
 */
public class AppException extends RuntimeException {

  private final AppErrorCode errorCode;

  /**
   * @param errorCode error classification, also drives the HTTP status
   */
  public AppException(AppErrorCode errorCode) {
    this(errorCode, errorCode.getDefaultMessage(), null);
  }

  /**
   * @param errorCode error classification, also drives the HTTP status
   * @param message message shown to the user, must not contain internal detail
   */
  public AppException(AppErrorCode errorCode, String message) {
    this(errorCode, message, null);
  }

  /**
   * @param errorCode error classification, also drives the HTTP status
   * @param message message shown to the user, must not contain internal detail
   * @param cause underlying failure, logged but never rendered
   */
  public AppException(AppErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * @return error classification carried by this exception
   */
  public AppErrorCode getErrorCode() {
    return errorCode;
  }
}
