package com.todolist.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue of application error codes.
 *
 * <p>Every {@link AppException} carries one of these. Add a new constant instead of throwing a
 * generic exception.
 */
public enum AppErrorCode {

  /** Request failed bean validation or a domain precondition. */
  INVALID_REQUEST("E0400", HttpStatus.BAD_REQUEST, "リクエストの内容が正しくありません。"),

  /** Request carries no credentials, or credentials that cannot be verified. */
  UNAUTHENTICATED("E0401", HttpStatus.UNAUTHORIZED, "認証が必要です。"),

  /** Supplied sign-in name or password did not match an account. */
  INVALID_CREDENTIALS("E0402", HttpStatus.UNAUTHORIZED,
      "ユーザー名またはパスワードが正しくありません。"),

  /** Caller is authenticated but not allowed to touch the requested resource. */
  ACCESS_DENIED("E0403", HttpStatus.FORBIDDEN, "この操作を行う権限がありません。"),

  /** Requested resource does not exist, or is not visible to the caller. */
  RESOURCE_NOT_FOUND("E0404", HttpStatus.NOT_FOUND, "対象のデータが見つかりません。"),

  /** Operation conflicts with the current state of the resource. */
  CONFLICT("E0409", HttpStatus.CONFLICT, "現在の状態では実行できません。"),

  /** Requested sign-in name is already registered. */
  DUPLICATE_USERNAME("E0410", HttpStatus.CONFLICT, "このユーザー名は既に使用されています。"),

  /** Requested mail address is already registered. */
  DUPLICATE_EMAIL("E0411", HttpStatus.CONFLICT, "このメールアドレスは既に使用されています。"),

  /** Account is locked after too many failed authentication attempts. */
  ACCOUNT_LOCKED("E0423", HttpStatus.LOCKED, "アカウントが一時的にロックされています。"),

  /** HTTP method is not supported on the requested path. */
  METHOD_NOT_ALLOWED("E0405", HttpStatus.METHOD_NOT_ALLOWED,
      "この操作は許可されていません。"),

  /** Request body media type is not supported. */
  UNSUPPORTED_MEDIA_TYPE("E0415", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      "サポートされていない形式です。"),

  /** Unexpected failure that is not attributable to the caller. */
  INTERNAL_ERROR("E0500", HttpStatus.INTERNAL_SERVER_ERROR, "システムエラーが発生しました。");

  private final String code;
  private final HttpStatus status;
  private final String defaultMessage;

  AppErrorCode(String code, HttpStatus status, String defaultMessage) {
    this.code = code;
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  /**
   * @return stable identifier safe to show to users and to grep for in logs
   */
  public String getCode() {
    return code;
  }

  /**
   * @return HTTP status this error maps to
   */
  public HttpStatus getStatus() {
    return status;
  }

  /**
   * @return user facing message with no internal detail
   */
  public String getDefaultMessage() {
    return defaultMessage;
  }
}
