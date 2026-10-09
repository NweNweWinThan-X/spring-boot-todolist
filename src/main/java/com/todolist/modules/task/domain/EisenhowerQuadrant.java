package com.todolist.modules.task.domain;

/**
 * Eisenhower matrix quadrant, derived from a task's urgency and importance.
 */
public enum EisenhowerQuadrant {

  /** Urgent and important: do it now. */
  DO,

  /** Important but not urgent: schedule it. */
  SCHEDULE,

  /** Urgent but not important: delegate it. */
  DELEGATE,

  /** Neither urgent nor important: drop it. */
  ELIMINATE;

  /**
   * @param urgent whether the task is time critical
   * @param important whether the task advances a goal
   * @return the quadrant the two axes place the task in
   */
  public static EisenhowerQuadrant of(boolean urgent, boolean important) {
    if (important) {
      return urgent ? DO : SCHEDULE;
    }
    return urgent ? DELEGATE : ELIMINATE;
  }
}
