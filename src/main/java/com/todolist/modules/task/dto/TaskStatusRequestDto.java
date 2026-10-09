package com.todolist.modules.task.dto;

import com.todolist.modules.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Input for the quick status update endpoint.
 *
 * @param status new lifecycle state
 */
public record TaskStatusRequestDto(@NotNull TaskStatus status) {
}
