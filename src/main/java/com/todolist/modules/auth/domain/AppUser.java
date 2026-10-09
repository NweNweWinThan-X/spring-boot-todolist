package com.todolist.modules.auth.domain;

import com.todolist.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Account that can sign in to the application.
 */
@Entity
@Table(name = "app_user")
@SequenceGenerator(name = "app_user_seq", sequenceName = "app_user_seq", allocationSize = 50)
public class AppUser extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_seq")
  private Long id;

  @Column(name = "username", nullable = false, length = 50, unique = true)
  private String username;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 72)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  /**
   * Required by JPA.
   */
  protected AppUser() {
  }

  /**
   * @param username unique sign-in name
   * @param email unique mail address, usable as an alternative sign-in name
   * @param passwordHash BCrypt hash, never a plain password
   * @param displayName name rendered in the UI
   * @param role authorisation role
   */
  public AppUser(String username, String email, String passwordHash, String displayName,
      Role role) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = role;
  }

  /**
   * @return surrogate key, {@code null} until the entity is persisted
   */
  public Long getId() {
    return id;
  }

  /**
   * @return unique sign-in name
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return mail address, or {@code null} for accounts created before it was required
   */
  public String getEmail() {
    return email;
  }

  /**
   * @return BCrypt hash of the account password
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * @return name rendered in the UI
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @return authorisation role
   */
  public Role getRole() {
    return role;
  }

  /**
   * @return whether the account may sign in
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * @param email new mail address
   */
  public void changeEmail(String email) {
    this.email = email;
  }

  /**
   * @param displayName new name to render in the UI
   */
  public void changeDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * @param passwordHash BCrypt hash of the new password
   */
  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Blocks further sign-in attempts without deleting the account.
   */
  public void disable() {
    this.enabled = false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AppUser other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
