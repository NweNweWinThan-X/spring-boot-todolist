package com.todolist.modules.task.dto;

import com.todolist.modules.label.dto.LabelDto;
import com.todolist.modules.task.domain.EisenhowerQuadrant;
import com.todolist.modules.task.domain.Task;
import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Read model for a task.
 *
 * @param id surrogate key
 * @param title short summary
 * @param description optional detail
 * @param status lifecycle state
 * @param priority urgency ranking
 * @param dueDate optional deadline
 * @param urgent whether the task is time critical
 * @param important whether the task advances a goal
 * @param quadrant Eisenhower quadrant derived from the two axes
 * @param startTime optional start of the time box
 * @param endTime optional end of the time box
 * @param alertEnabled whether the owner asked to be reminded
 * @param projectId owning project, {@code null} for the Inbox
 * @param projectName owning project's name, {@code null} for the Inbox
 * @param labels labels attached to the task
 * @param userId owning account
 * @param createdAt instant the task was created
 * @param updatedAt instant the task was last modified
 */
public record TaskResponseDto(Long id, String title, String description, TaskStatus status,
    TaskPriority priority, LocalDate dueDate, boolean urgent, boolean important,
    EisenhowerQuadrant quadrant, LocalTime startTime, LocalTime endTime,
    boolean alertEnabled, Long projectId, String projectName, List<LabelDto> labels,
    Long userId, Instant createdAt, Instant updatedAt) {

  /**
   * Reads the owner identifier from the foreign key, so the account association is not loaded.
   * The project and labels must already be initialised by the query that fetched the task.
   *
   * @param task persisted task to project
   * @return read model built from the entity
   */
  public static TaskResponseDto from(Task task) {
    return new TaskResponseDto(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getPriority(),
        task.getDueDate(),
        task.isUrgent(),
        task.isImportant(),
        task.getQuadrant(),
        task.getStartTime(),
        task.getEndTime(),
        task.isAlertEnabled(),
        task.getProject() == null ? null : task.getProject().getId(),
        task.getProject() == null ? null : task.getProject().getName(),
        task.getLabels().stream().map(LabelDto::from).toList(),
        task.getUser().getId(),
        task.getCreatedAt(),
        task.getUpdatedAt()
    );
  }
}
