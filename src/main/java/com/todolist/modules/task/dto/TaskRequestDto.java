package com.todolist.modules.task.dto;

import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Create and update input for a task.
 *
 * <p>The text patterns are allowlists: letters, digits, spaces, punctuation and currency or maths
 * symbols are accepted, which covers Japanese input, while angle brackets and control characters
 * are rejected.
 *
 * @param title short summary
 * @param description optional detail
 * @param status lifecycle state
 * @param priority urgency ranking
 * @param dueDate optional deadline
 * @param urgent whether the task is time critical
 * @param important whether the task advances a goal
 * @param startTime optional start of the time box
 * @param endTime optional end of the time box, must be after the start
 * @param alertEnabled whether the owner wants a reminder
 * @param projectId owning project, {@code null} to leave the task in the Inbox
 * @param labelIds labels to attach, empty for none
 */
public record TaskRequestDto(

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{Zs}\\p{P}\\p{Sm}\\p{Sc}&&[^<>]]+$",
        message = "使用できない文字が含まれています")
    String title,

    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{Zs}\\p{P}\\p{Sm}\\p{Sc}\\r\\n&&[^<>]]*$",
        message = "使用できない文字が含まれています")
    String description,

    @NotNull
    TaskStatus status,

    @NotNull
    TaskPriority priority,

    LocalDate dueDate,

    Boolean urgent,

    Boolean important,

    LocalTime startTime,

    LocalTime endTime,

    Boolean alertEnabled,

    Long projectId,

    Set<Long> labelIds) {

  /**
   * Normalises optional fields so an omitted flag means "false" rather than a deserialisation
   * failure. Jackson 3 rejects a null for a primitive by default, so these are boxed and
   * defaulted here instead.
   *
   * @param title short summary
   * @param description optional detail
   * @param status lifecycle state
   * @param priority urgency ranking
   * @param dueDate optional deadline
   * @param urgent whether the task is time critical
   * @param important whether the task advances a goal
   * @param startTime optional start of the time box
   * @param endTime optional end of the time box
   * @param alertEnabled whether the owner wants a reminder
   * @param projectId owning project
   * @param labelIds labels to attach
   */
  public TaskRequestDto {
    urgent = urgent != null && urgent;
    important = important != null && important;
    alertEnabled = alertEnabled != null && alertEnabled;
    labelIds = labelIds == null ? Set.of() : Set.copyOf(labelIds);
  }

  /**
   * @return whether the time box is absent or correctly ordered
   */
  @AssertTrue(message = "終了時刻は開始時刻より後にしてください")
  public boolean isTimeRangeValid() {
    return startTime == null || endTime == null || endTime.isAfter(startTime);
  }
}
