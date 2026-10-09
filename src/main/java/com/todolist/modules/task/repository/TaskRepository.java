package com.todolist.modules.task.repository;

import com.todolist.modules.task.domain.Task;
import com.todolist.modules.task.domain.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

/**
 * Task persistence.
 *
 * <p>Every finder is scoped by owner, so a task cannot be reached through the repository without
 * naming the account it belongs to.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  /**
   * Fetches project and labels alongside the page, so rendering a list never issues a query per
   * row.
   *
   * @param specification filters to apply
   * @param pageable page and sort
   * @return page of tasks with their associations initialised
   */
  @Override
  @EntityGraph(attributePaths = {"project", "labels"})
  Page<Task> findAll(Specification<Task> specification, Pageable pageable);

  /**
   * @param userId owning account
   * @return active tasks belonging to the account
   */
  @Query("""
      select t
      from Task t
      where t.user.id = :userId
        and t.deleted = false
      """)
  List<Task> findByUserId(@Param("userId") Long userId);

  /**
   * @param userId owning account
   * @param status lifecycle state to match
   * @return active tasks belonging to the account in the given state
   */
  @Query("""
      select t
      from Task t
      where t.user.id = :userId
        and t.status = :status
        and t.deleted = false
      """)
  List<Task> findByUserIdAndStatus(@Param("userId") Long userId,
      @Param("status") TaskStatus status);

  /**
   * @param id task identifier
   * @param userId owning account
   * @return the task with its associations, when it exists and belongs to the account
   */
  @EntityGraph(attributePaths = {"project", "labels"})
  @Query("""
      select t
      from Task t
      where t.id = :id
        and t.user.id = :userId
        and t.deleted = false
      """)
  Optional<Task> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * @param userId owning account
   * @param date day to summarise
   * @return number of active tasks due on the day
   */
  @Query("""
      select count(t)
      from Task t
      where t.user.id = :userId
        and t.dueDate = :date
        and t.deleted = false
      """)
  long countDueOn(@Param("userId") Long userId, @Param("date") LocalDate date);

  /**
   * @param userId owning account
   * @param date day to summarise
   * @return number of completed tasks due on the day
   */
  @Query("""
      select count(t)
      from Task t
      where t.user.id = :userId
        and t.dueDate = :date
        and t.status = com.todolist.modules.task.domain.TaskStatus.COMPLETED
        and t.deleted = false
      """)
  long countCompletedDueOn(@Param("userId") Long userId, @Param("date") LocalDate date);

  /**
   * Clears the project reference from every task that pointed at a deleted project, so the tasks
   * fall back to the Inbox instead of disappearing.
   *
   * @param projectId project being removed
   * @param userId owning account
   * @return number of tasks moved to the Inbox
   */
  @org.springframework.data.jpa.repository.Modifying
  @Query("""
      update Task t
      set t.project = null
      where t.project.id = :projectId
        and t.user.id = :userId
      """)
  int detachFromProject(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
