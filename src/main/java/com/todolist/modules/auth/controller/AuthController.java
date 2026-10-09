package com.todolist.modules.auth.controller;

import com.todolist.modules.auth.dto.AuthResponseDto;
import com.todolist.modules.auth.dto.AuthUserDto;
import com.todolist.modules.auth.dto.LoginRequestDto;
import com.todolist.modules.auth.dto.RegisterRequestDto;
import com.todolist.modules.auth.service.AuthService;
import com.todolist.modules.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final CurrentUserService currentUserService;

  /**
   * @param authService registration and sign-in logic
   * @param currentUserService resolves the caller for the profile endpoint
   */
  public AuthController(AuthService authService, CurrentUserService currentUserService) {
    this.authService = authService;
    this.currentUserService = currentUserService;
  }

  /**
   * @param request sign-up input
   * @return created account with an access token
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  /**
   * @param request sign-in input
   * @return account with an access token
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
    return ResponseEntity.ok(authService.login(request));
  }

  /**
   * Lets the client restore its session from a stored token without keeping user data locally.
   *
   * @return summary of the authenticated account
   */
  @GetMapping("/me")
  public ResponseEntity<AuthUserDto> me() {
    return ResponseEntity.ok(AuthUserDto.from(currentUserService.requireCurrentUser()));
  }
}
