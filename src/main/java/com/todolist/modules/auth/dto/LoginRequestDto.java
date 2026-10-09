package com.todolist.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sign-in input.
 *
 * @param usernameOrEmail sign-in name or mail address
 * @param password plain password
 */
public record LoginRequestDto(

    @NotBlank
    @Size(max = 255)
    String usernameOrEmail,

    @NotBlank
    @Size(max = 100)
    String password) {
}
