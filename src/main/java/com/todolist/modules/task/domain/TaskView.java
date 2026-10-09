package com.todolist.modules.task.domain;

/**
 * Built-in ways of grouping tasks.
 *
 * <p>These are derived from task attributes rather than stored, so they cannot be created,
 * renamed or deleted.
 */
public enum TaskView {

  /** Every active task. */
  ALL,

  /** Tasks with no project. */
  INBOX,

  /** Tasks due today. */
  TODAY,

  /** Unfinished tasks whose due date has passed. */
  OVERDUE,

  /** Tasks due within the next seven days. */
  UPCOMING
}
