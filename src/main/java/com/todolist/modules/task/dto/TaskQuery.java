package com.todolist.modules.task.dto;

import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.domain.TaskView;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;

/**
 * Query parameters for the task list.
 *
 * @param view built-in view to apply
 * @param status lifecycle state filter
 * @param priority urgency ranking filter
 * @param projectId owning project filter
 * @param labelId attached label filter
 * @param urgent Eisenhower urgency axis filter
 * @param important Eisenhower importance axis filter
 * @param from inclusive lower bound on the due date
 * @param to inclusive upper bound on the due date
 * @param search free text matched against the title
 * @param sortBy sortable field name
 * @param direction sort direction
 * @param page zero based page index
 * @param size page size
 */
public record TaskQuery(TaskView view, TaskStatus status, TaskPriority priority, Long projectId,
    Long labelId, Boolean urgent, Boolean important, LocalDate from, LocalDate to, String search,
    String sortBy, Sort.Direction direction, int page, int size) {
}
