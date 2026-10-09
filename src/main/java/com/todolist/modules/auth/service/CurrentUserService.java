package com.todolist.modules.auth.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.repository.AppUserRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the account behind the current security context.
 *
 * <p>Every per-user query goes through this rather than trusting a client supplied identifier,
 * which is what keeps one account from reading another account's rows.
 */
@Service
@Transactional(readOnly = true)
public class CurrentUserService {

  private static final String ANONYMOUS = "anonymousUser";

  private final AppUserRepository repository;

  /**
   * @param repository account persistence
   */
  public CurrentUserService(AppUserRepository repository) {
    this.repository = repository;
  }

  /**
   * @return account behind the current security context
   * @throws AppException when the request is anonymous, or the account no longer exists
   */
  public AppUser requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AppException(AppErrorCode.UNAUTHENTICATED);
    }
    String username = resolveUsername(authentication);
    if (ANONYMOUS.equals(username)) {
      throw new AppException(AppErrorCode.UNAUTHENTICATED);
    }
    return repository.findActiveByUsername(username)
        .orElseThrow(() -> new AppException(AppErrorCode.UNAUTHENTICATED));
  }

  private String resolveUsername(Authentication authentication) {
    if (authentication.getPrincipal() instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }
    return authentication.getName();
  }
}
