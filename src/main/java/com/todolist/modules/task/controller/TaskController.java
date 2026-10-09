package com.todolist.modules.task.controller;

import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.domain.TaskView;
import com.todolist.modules.task.dto.DailyProgressDto;
import com.todolist.modules.task.dto.TaskQuery;
import com.todolist.modules.task.dto.TaskRequestDto;
import com.todolist.modules.task.dto.TaskResponseDto;
import com.todolist.modules.task.dto.TaskStatusRequestDto;
import com.todolist.modules.task.service.TaskService;
import com.todolist.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task endpoints. Every request is scoped to the authenticated account by the service layer.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private static final String DEFAULT_PAGE = "0";
  private static final String DEFAULT_SIZE = "20";
  private static final String DEFAULT_DIRECTION = "DESC";
  private static final String DEFAULT_VIEW = "ALL";

  private final TaskService taskService;

  /**
   * @param taskService task business logic
   */
  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  /**
   * @param view built-in view: ALL, INBOX, TODAY, OVERDUE or UPCOMING
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
   * @return page of the caller's tasks
   */
  @GetMapping
  public ResponseEntity<PagedResponse<TaskResponseDto>> list(
      @RequestParam(defaultValue = DEFAULT_VIEW) TaskView view,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long labelId,
      @RequestParam(required = false) Boolean urgent,
      @RequestParam(required = false) Boolean important,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String sortBy,
      @RequestParam(defaultValue = DEFAULT_DIRECTION) Sort.Direction direction,
      @RequestParam(defaultValue = DEFAULT_PAGE) int page,
      @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
    TaskQuery query = new TaskQuery(view, status, priority, projectId, labelId, urgent, important,
        from, to, search, sortBy, direction, page, size);
    return ResponseEntity.ok(PagedResponse.from(taskService.findTasks(query)));
  }

  /**
   * @param date day to summarise, defaults to today
   * @return completion counts for the day
   */
  @GetMapping("/progress")
  public ResponseEntity<DailyProgressDto> progress(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(taskService.findDailyProgress(date));
  }

  /**
   * @param id task identifier
   * @return the requested task
   */
  @GetMapping("/{id}")
  public ResponseEntity<TaskResponseDto> findById(@PathVariable Long id) {
    return ResponseEntity.ok(taskService.findById(id));
  }

  /**
   * @param request fields of the new task
   * @return the created task, with its location header
   */
  @PostMapping
  public ResponseEntity<TaskResponseDto> create(@Valid @RequestBody TaskRequestDto request) {
    TaskResponseDto created = taskService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
  }

  /**
   * @param id task identifier
   * @param request replacement fields
   * @return the updated task
   */
  @PutMapping("/{id}")
  public ResponseEntity<TaskResponseDto> update(@PathVariable Long id,
      @Valid @RequestBody TaskRequestDto request) {
    return ResponseEntity.ok(taskService.update(id, request));
  }

  /**
   * @param id task identifier
   * @param request new lifecycle state
   * @return the updated task
   */
  @PatchMapping("/{id}/status")
  public ResponseEntity<TaskResponseDto> updateStatus(@PathVariable Long id,
      @Valid @RequestBody TaskStatusRequestDto request) {
    return ResponseEntity.ok(taskService.updateStatus(id, request.status()));
  }

  /**
   * @param id task identifier
   * @return empty 204 response
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
