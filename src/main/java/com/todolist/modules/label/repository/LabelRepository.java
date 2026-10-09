package com.todolist.modules.label.repository;

import com.todolist.modules.label.domain.Label;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Label persistence, always scoped by owner.
 */
public interface LabelRepository extends JpaRepository<Label, Long> {

  /**
   * @param userId owning account
   * @return active labels ordered by name
   */
  @Query("""
      select l
      from Label l
      where l.user.id = :userId
        and l.deleted = false
      order by l.name
      """)
  List<Label> findByUserId(@Param("userId") Long userId);

  /**
   * @param id label identifier
   * @param userId owning account
   * @return the label when it exists and belongs to the account, otherwise empty
   */
  @Query("""
      select l
      from Label l
      where l.id = :id
        and l.user.id = :userId
        and l.deleted = false
      """)
  Optional<Label> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * Resolves a set of identifiers in one statement, so attaching labels never issues one query
   * per identifier.
   *
   * @param ids label identifiers to resolve
   * @param userId owning account
   * @return the labels among the identifiers that belong to the account
   */
  @Query("""
      select l
      from Label l
      where l.id in :ids
        and l.user.id = :userId
        and l.deleted = false
      """)
  Set<Label> findAllByIdsAndUserId(@Param("ids") Set<Long> ids, @Param("userId") Long userId);

  /**
   * @param userId owning account
   * @param name display name, compared case-insensitively
   * @return whether the account already has an active label with the name
   */
  @Query("""
      select count(l) > 0
      from Label l
      where l.user.id = :userId
        and lower(l.name) = lower(:name)
        and l.deleted = false
      """)
  boolean existsByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
}
