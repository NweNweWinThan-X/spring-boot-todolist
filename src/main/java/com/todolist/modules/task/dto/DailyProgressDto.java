package com.todolist.modules.task.dto;

import java.time.LocalDate;

/**
 * Completion summary for a single day, shown by the dashboard's progress card.
 *
 * @param date day being summarised
 * @param total active tasks due that day
 * @param completed how many of them are done
 * @param percentage completion as a whole percentage, zero when nothing is due
 */
public record DailyProgressDto(LocalDate date, long total, long completed, int percentage) {

  /**
   * @param date day being summarised
   * @param total active tasks due that day
   * @param completed how many of them are done
   * @return summary with the percentage derived from the counts
   */
  public static DailyProgressDto of(LocalDate date, long total, long completed) {
    int percentage = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
    return new DailyProgressDto(date, total, completed, percentage);
  }
}
