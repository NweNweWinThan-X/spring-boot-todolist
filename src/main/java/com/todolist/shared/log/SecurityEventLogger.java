package com.todolist.shared.log;

import com.todolist.modules.auth.service.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Records authentication and authorisation outcomes (security audit No.21).
 *
 * <p>Only identifiers are logged. Passwords, mail addresses and other personal data must never
 * reach the log.
 */
@Component
public class SecurityEventLogger {

  private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

  private final LoginAttemptService loginAttemptService;

  /**
   * @param loginAttemptService brute force counters updated on each outcome
   */
  public SecurityEventLogger(LoginAttemptService loginAttemptService) {
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * @param event successful authentication published by Spring Security
   */
  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    String username = event.getAuthentication().getName();
    loginAttemptService.reset(username);
    log.info("ログイン成功: userId={}", username);
  }

  /**
   * @param event failed authentication published by Spring Security
   */
  @EventListener
  public void onFailure(AbstractAuthenticationFailureEvent event) {
    String username = String.valueOf(event.getAuthentication().getName());
    loginAttemptService.recordFailure(username);
    log.warn("ログイン失敗: userId={}, reason={}", username,
        event.getException().getClass().getSimpleName());
  }

  /**
   * @param event authorisation denial published by Spring Security
   */
  @EventListener
  public void onDenied(AuthorizationDeniedEvent<?> event) {
    log.warn("アクセス拒否: userId={}", event.getAuthentication().get().getName());
  }
}
