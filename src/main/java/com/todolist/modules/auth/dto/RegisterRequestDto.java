package com.todolist.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sign-up input.
 *
 * @param username sign-in name, restricted to an allowlist of safe characters
 * @param email mail address used as an alternative sign-in name
 * @param password plain password, hashed before it is stored
 */
public record RegisterRequestDto(

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
        message = "半角英数字と . _ - のみ使用できます")
    String username,

    @NotBlank
    @Email
    @Size(max = 255)
    String email,

    @NotBlank
    @Size(min = 8, max = 100)
    String password) {
}
