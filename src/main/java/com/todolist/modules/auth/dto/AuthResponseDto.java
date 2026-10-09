package com.todolist.modules.auth.dto;

import com.todolist.modules.auth.domain.AppUser;

/**
 * Issued access token plus the account summary the client renders.
 *
 * @param accessToken signed bearer token
 * @param tokenType always {@code Bearer}
 * @param expiresIn token lifetime in seconds
 * @param user account summary
 */
public record AuthResponseDto(String accessToken, String tokenType, long expiresIn,
    AuthUserDto user) {

  private static final String BEARER = "Bearer";

  /**
   * @param accessToken signed bearer token
   * @param expiresIn token lifetime in seconds
   * @param user authenticated account
   * @return response carrying the token and the account summary
   */
  public static AuthResponseDto of(String accessToken, long expiresIn, AppUser user) {
    return new AuthResponseDto(accessToken, BEARER, expiresIn, AuthUserDto.from(user));
  }
}
