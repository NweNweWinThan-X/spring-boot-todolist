package com.todolist.modules.auth.service;

import com.todolist.shared.exception.AppErrorCode;
import org.springframework.security.authentication.LockedException;

/**
 * Raised when sign-in is refused because the brute force threshold was reached.
 */
public class LockedAccountException extends LockedException {

  /**
   * Builds the exception with the user facing message of
   * {@link AppErrorCode#ACCOUNT_LOCKED}.
   */
  public LockedAccountException() {
    super(AppErrorCode.ACCOUNT_LOCKED.getDefaultMessage());
  }
}
