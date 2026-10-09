package com.todolist.modules.auth.repository;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.dto.AppUserDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Account persistence. Read queries project straight into DTOs.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  /**
   * @param username sign-in name to look up
   * @return matching active account, or empty when absent or soft deleted
   */
  @Query("""
      select u
      from AppUser u
      where u.username = :username
        and u.deleted = false
      """)
  Optional<AppUser> findActiveByUsername(@Param("username") String username);

  /**
   * Accepts either credential so the sign-in form can take a name or a mail address.
   *
   * @param identifier sign-in name or mail address, compared case-insensitively for mail
   * @return matching active account, or empty when absent or soft deleted
   */
  @Query("""
      select u
      from AppUser u
      where (u.username = :identifier or lower(u.email) = lower(:identifier))
        and u.deleted = false
      """)
  Optional<AppUser> findActiveByUsernameOrEmail(@Param("identifier") String identifier);

  /**
   * @param username sign-in name to test
   * @return whether an active account already uses the name
   */
  @Query("""
      select count(u) > 0
      from AppUser u
      where u.username = :username
        and u.deleted = false
      """)
  boolean existsActiveByUsername(@Param("username") String username);

  /**
   * @param email mail address to test, compared case-insensitively
   * @return whether an active account already uses the mail address
   */
  @Query("""
      select count(u) > 0
      from AppUser u
      where lower(u.email) = lower(:email)
        and u.deleted = false
      """)
  boolean existsActiveByEmail(@Param("email") String email);

  /**
   * @return read models for every active account, ordered by sign-in name
   */
  @Query("""
      select new com.todolist.modules.auth.dto.AppUserDto(
        u.id,
        u.username,
        u.displayName,
        u.role
      )
      from AppUser u
      where u.deleted = false
      order by u.username
      """)
  List<AppUserDto> findAllActiveDto();
}
