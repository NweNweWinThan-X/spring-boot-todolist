package com.todolist.modules.auth.dto;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.domain.Role;

/**
 * Account summary returned to the signed-in client.
 *
 * <p>The mail address is included because the dashboard renders it; no other personal data is
 * exposed.
 *
 * @param id surrogate key of the account
 * @param username sign-in name
 * @param email mail address
 * @param displayName name rendered in the UI
 * @param role authorisation role
 */
public record AuthUserDto(Long id, String username, String email, String displayName, Role role) {

  /**
   * @param user persisted account to project
   * @return summary built from the entity
   */
  public static AuthUserDto from(AppUser user) {
    return new AuthUserDto(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getDisplayName(),
        user.getRole()
    );
  }
}
