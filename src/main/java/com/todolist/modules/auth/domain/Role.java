package com.todolist.modules.auth.domain;

/**
 * Authorisation roles. Stored without the {@code ROLE_} prefix Spring Security adds at runtime.
 */
public enum Role {

  /** Ordinary end user. */
  USER,

  /** Administrator, allowed to reach management screens. */
  ADMIN;

  /**
   * @return authority name expected by Spring Security expressions
   */
  public String getAuthority() {
    return "ROLE_" + name();
  }
}
