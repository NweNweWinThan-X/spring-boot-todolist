package com.todolist.modules.auth.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.domain.Role;
import com.todolist.modules.auth.dto.AuthResponseDto;
import com.todolist.modules.auth.dto.LoginRequestDto;
import com.todolist.modules.auth.dto.RegisterRequestDto;
import com.todolist.modules.auth.repository.AppUserRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import com.todolist.shared.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and sign-in, both of which answer with an access token.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AppUserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final AuthenticationManager authenticationManager;

  /**
   * @param repository account persistence
   * @param passwordEncoder hashes passwords before they are stored
   * @param tokenProvider issues access tokens
   * @param authenticationManager verifies supplied credentials
   */
  public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder,
      JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
    this.authenticationManager = authenticationManager;
  }

  /**
   * @param request sign-up input
   * @return access token and account summary for the newly created account
   * @throws AppException when the sign-in name or mail address is already registered
   */
  @Transactional
  public AuthResponseDto register(RegisterRequestDto request) {
    if (repository.existsActiveByUsername(request.username())) {
      throw new AppException(AppErrorCode.DUPLICATE_USERNAME);
    }
    if (repository.existsActiveByEmail(request.email())) {
      throw new AppException(AppErrorCode.DUPLICATE_EMAIL);
    }
    AppUser user = repository.save(new AppUser(
        request.username(),
        request.email(),
        passwordEncoder.encode(request.password()),
        request.username(),
        Role.USER
    ));
    log.info("アカウント登録: userId={}", user.getUsername());
    return issueToken(user);
  }

  /**
   * @param request sign-in input
   * @return access token and account summary
   * @throws AppException when the credentials do not match an account
   */
  public AuthResponseDto login(LoginRequestDto request) {
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
          request.usernameOrEmail(), request.password()));
    } catch (AuthenticationException e) {
      log.warn("ログイン失敗: userId={}, reason={}", request.usernameOrEmail(),
          e.getClass().getSimpleName());
      throw new AppException(AppErrorCode.INVALID_CREDENTIALS);
    }
    AppUser user = repository.findActiveByUsernameOrEmail(request.usernameOrEmail())
        .orElseThrow(() -> new AppException(AppErrorCode.INVALID_CREDENTIALS));
    log.info("ログイン成功: userId={}", user.getUsername());
    return issueToken(user);
  }

  private AuthResponseDto issueToken(AppUser user) {
    return AuthResponseDto.of(
        tokenProvider.generateToken(user.getUsername()),
        tokenProvider.getExpiresInSeconds(),
        user
    );
  }
}
