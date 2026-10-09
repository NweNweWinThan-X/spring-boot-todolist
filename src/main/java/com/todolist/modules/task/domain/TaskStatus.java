package com.todolist.modules.task.domain;

/**
 * Lifecycle state of a task.
 */
public enum TaskStatus {

  /** Not started. */
  PENDING,

  /** Started but not finished. */
  IN_PROGRESS,

  /** Finished. */
  COMPLETED
}
