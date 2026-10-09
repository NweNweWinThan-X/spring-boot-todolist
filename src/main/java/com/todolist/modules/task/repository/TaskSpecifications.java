package com.todolist.modules.task.repository;

import com.todolist.modules.task.domain.Task;
import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.domain.TaskView;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable predicates for the task list query.
 *
 * <p>Criteria are used rather than a JPQL query with {@code :param is null} tests, because those
 * tests need an explicit cast for enum parameters on PostgreSQL.
 */
public final class TaskSpecifications {

  private static final String FIELD_USER = "user";
  private static final String FIELD_PROJECT = "project";
  private static final String FIELD_LABELS = "labels";
  private static final String FIELD_ID = "id";
  private static final String FIELD_DELETED = "deleted";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_PRIORITY = "priority";
  private static final String FIELD_TITLE = "title";
  private static final String FIELD_DUE_DATE = "dueDate";
  private static final String FIELD_URGENT = "urgent";
  private static final String FIELD_IMPORTANT = "important";

  private static final int UPCOMING_DAYS = 7;

  private TaskSpecifications() {
  }

  /**
   * @param userId owning account
   * @return predicate limiting results to the account's own rows that are not soft deleted
   */
  public static Specification<Task> ownedBy(Long userId) {
    return (root, query, builder) -> builder.and(
        builder.equal(root.get(FIELD_USER).get(FIELD_ID), userId),
        builder.isFalse(root.get(FIELD_DELETED))
    );
  }

  /**
   * @param status lifecycle state to match, {@code null} to skip
   * @return predicate on status, or a no-op when the status is absent
   */
  public static Specification<Task> hasStatus(TaskStatus status) {
    return (root, query, builder) ->
        status == null ? builder.conjunction() : builder.equal(root.get(FIELD_STATUS), status);
  }

  /**
   * @param priority urgency ranking to match, {@code null} to skip
   * @return predicate on priority, or a no-op when the priority is absent
   */
  public static Specification<Task> hasPriority(TaskPriority priority) {
    return (root, query, builder) -> priority == null
        ? builder.conjunction()
        : builder.equal(root.get(FIELD_PRIORITY), priority);
  }

  /**
   * @param keyword free text matched against the title, {@code null} or blank to skip
   * @return case-insensitive title predicate, or a no-op when the keyword is absent
   */
  public static Specification<Task> titleContains(String keyword) {
    return (root, query, builder) -> {
      if (StringUtils.isBlank(keyword)) {
        return builder.conjunction();
      }
      return builder.like(builder.lower(root.get(FIELD_TITLE)),
          "%" + keyword.trim().toLowerCase() + "%");
    };
  }

  /**
   * @param projectId project to match, {@code null} to skip
   * @return predicate on the owning project, or a no-op when absent
   */
  public static Specification<Task> inProject(Long projectId) {
    return (root, query, builder) -> projectId == null
        ? builder.conjunction()
        : builder.equal(root.get(FIELD_PROJECT).get(FIELD_ID), projectId);
  }

  /**
   * Joins the label association, so a task is returned when any of its labels matches.
   *
   * @param labelId label to match, {@code null} to skip
   * @return predicate on an attached label, or a no-op when absent
   */
  public static Specification<Task> hasLabel(Long labelId) {
    return (root, query, builder) -> {
      if (labelId == null) {
        return builder.conjunction();
      }
      if (query != null) {
        query.distinct(true);
      }
      return builder.equal(root.join(FIELD_LABELS, JoinType.INNER).get(FIELD_ID), labelId);
    };
  }

  /**
   * @param urgent urgency axis to match, {@code null} to skip
   * @param important importance axis to match, {@code null} to skip
   * @return predicate selecting one Eisenhower quadrant, or a no-op when either axis is absent
   */
  public static Specification<Task> inQuadrant(Boolean urgent, Boolean important) {
    return (root, query, builder) -> {
      if (urgent == null || important == null) {
        return builder.conjunction();
      }
      return builder.and(
          builder.equal(root.get(FIELD_URGENT), urgent),
          builder.equal(root.get(FIELD_IMPORTANT), important)
      );
    };
  }

  /**
   * Applies one of the built-in views, each derived from task attributes rather than stored.
   *
   * @param view view to apply, {@code null} or {@link TaskView#ALL} to skip
   * @param today the caller's current date
   * @return predicate implementing the view
   */
  public static Specification<Task> inView(TaskView view, LocalDate today) {
    return (root, query, builder) -> {
      if (view == null || view == TaskView.ALL) {
        return builder.conjunction();
      }
      return switch (view) {
        case INBOX -> builder.isNull(root.get(FIELD_PROJECT));
        case TODAY -> builder.equal(root.get(FIELD_DUE_DATE), today);
        case OVERDUE -> builder.and(
            builder.lessThan(root.get(FIELD_DUE_DATE), today),
            builder.notEqual(root.get(FIELD_STATUS), TaskStatus.COMPLETED));
        case UPCOMING -> builder.between(root.get(FIELD_DUE_DATE), today,
            today.plusDays(UPCOMING_DAYS));
        default -> builder.conjunction();
      };
    };
  }

  /**
   * @param from inclusive lower bound, {@code null} to skip
   * @param to inclusive upper bound, {@code null} to skip
   * @return predicate on the due date range, used by the week strip
   */
  public static Specification<Task> dueBetween(LocalDate from, LocalDate to) {
    return (root, query, builder) -> {
      if (from == null && to == null) {
        return builder.conjunction();
      }
      if (from == null) {
        return builder.lessThanOrEqualTo(root.get(FIELD_DUE_DATE), to);
      }
      if (to == null) {
        return builder.greaterThanOrEqualTo(root.get(FIELD_DUE_DATE), from);
      }
      return builder.between(root.get(FIELD_DUE_DATE), from, to);
    };
  }
}
