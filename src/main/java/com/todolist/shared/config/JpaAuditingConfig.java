package com.todolist.shared.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Populates the audit columns declared on
 * {@link com.todolist.shared.domain.AuditableEntity}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

  /**
   * @return current username, or empty for unauthenticated and system writes
   */
  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        return Optional.empty();
      }
      return Optional.ofNullable(authentication.getName());
    };
  }
}
