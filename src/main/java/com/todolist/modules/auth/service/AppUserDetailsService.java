package com.todolist.modules.auth.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.repository.AppUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads accounts for Spring Security and refuses accounts that are temporarily locked out.
 *
 * <p>Accepts a sign-in name or a mail address so both work on the sign-in form.
 */
@Service
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository repository;
  private final LoginAttemptService loginAttemptService;

  /**
   * @param repository account persistence
   * @param loginAttemptService brute force counters
   */
  public AppUserDetailsService(AppUserRepository repository,
      LoginAttemptService loginAttemptService) {
    this.repository = repository;
    this.loginAttemptService = loginAttemptService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    if (loginAttemptService.isBlocked(username)) {
      throw new LockedAccountException();
    }
    AppUser user = repository.findActiveByUsernameOrEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("account not found"));
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(List.of(new SimpleGrantedAuthority(user.getRole().getAuthority())))
        .disabled(!user.isEnabled())
        .build();
  }
}
