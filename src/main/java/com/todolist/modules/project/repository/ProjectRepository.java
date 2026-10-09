package com.todolist.modules.project.repository;

import com.todolist.modules.project.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Project persistence, always scoped by owner.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

  /**
   * @param userId owning account
   * @return active projects, archived ones last, then by name
   */
  @Query("""
      select p
      from Project p
      where p.user.id = :userId
        and p.deleted = false
      order by p.archived, p.name
      """)
  List<Project> findByUserId(@Param("userId") Long userId);

  /**
   * @param id project identifier
   * @param userId owning account
   * @return the project when it exists and belongs to the account, otherwise empty
   */
  @Query("""
      select p
      from Project p
      where p.id = :id
        and p.user.id = :userId
        and p.deleted = false
      """)
  Optional<Project> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * @param userId owning account
   * @param name display name, compared case-insensitively
   * @return whether the account already has an active project with the name
   */
  @Query("""
      select count(p) > 0
      from Project p
      where p.user.id = :userId
        and lower(p.name) = lower(:name)
        and p.deleted = false
      """)
  boolean existsByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
}
