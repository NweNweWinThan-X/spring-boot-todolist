package com.todolist.modules.auth.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.domain.Role;
import com.todolist.modules.auth.repository.AppUserRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator from environment variables so no credential is committed.
 *
 * <p>Active in the {@code local} and {@code h2} profiles only; other environments seed accounts
 * out of band.
 */
@Component
@Profile({"local", "h2"})
public class AdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

  private final AppUserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final String adminUsername;
  private final String adminPassword;
  private final String adminEmail;

  /**
   * @param repository account persistence
   * @param passwordEncoder encoder used to hash the bootstrap password
   * @param adminUsername sign-in name taken from {@code APP_ADMIN_USERNAME}
   * @param adminPassword plain password taken from {@code APP_ADMIN_PASSWORD}
   * @param adminEmail mail address taken from {@code APP_ADMIN_EMAIL}, may be blank
   */
  public AdminBootstrapRunner(AppUserRepository repository, PasswordEncoder passwordEncoder,
      @Value("${app.bootstrap.admin-username:}") String adminUsername,
      @Value("${app.bootstrap.admin-password:}") String adminPassword,
      @Value("${app.bootstrap.admin-email:}") String adminEmail) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
    this.adminEmail = adminEmail;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminUsername.isBlank() || adminPassword.isBlank()) {
      log.info("管理者ブートストラップをスキップ: 環境変数が未設定");
      return;
    }
    Preconditions.requireNotBlank(adminUsername, AppErrorCode.INVALID_REQUEST);
    if (repository.findActiveByUsername(adminUsername).isPresent()) {
      return;
    }
    AppUser admin = new AppUser(
        adminUsername,
        adminEmail.isBlank() ? null : adminEmail,
        passwordEncoder.encode(adminPassword),
        adminUsername,
        Role.ADMIN
    );
    repository.save(admin);
    log.info("管理者アカウントを作成: userId={}", adminUsername);
  }
}
