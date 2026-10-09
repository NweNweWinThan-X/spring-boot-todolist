package com.todolist.modules.auth.dto;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.domain.Role;

/**
 * Read model for account screens. Carries no password material.
 *
 * @param id surrogate key of the account
 * @param username unique sign-in name
 * @param displayName name rendered in the UI
 * @param role authorisation role
 */
public record AppUserDto(Long id, String username, String displayName, Role role) {

  /**
   * @param user persisted account to project
   * @return read model built from the entity
   */
  public static AppUserDto from(AppUser user) {
    return new AppUserDto(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getRole()
    );
  }
}
