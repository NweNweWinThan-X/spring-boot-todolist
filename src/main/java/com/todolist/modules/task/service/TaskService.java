package com.todolist.modules.task.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.service.CurrentUserService;
import com.todolist.modules.label.service.LabelService;
import com.todolist.modules.project.domain.Project;
import com.todolist.modules.project.service.ProjectService;
import com.todolist.modules.task.domain.Task;
import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.domain.TaskView;
import com.todolist.modules.task.dto.DailyProgressDto;
import com.todolist.modules.task.dto.TaskQuery;
import com.todolist.modules.task.dto.TaskRequestDto;
import com.todolist.modules.task.dto.TaskResponseDto;
import com.todolist.modules.task.repository.TaskRepository;
import com.todolist.modules.task.repository.TaskSpecifications;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task business logic.
 *
 * <p>The owning account always comes from the security context, never from the request, so one
 * account cannot read or modify another account's tasks.
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

  private static final Set<String> SORTABLE_FIELDS = Set.of(
      "id", "title", "status", "priority", "dueDate", "startTime", "createdAt", "updatedAt");
  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final int MAX_PAGE_SIZE = 100;

  private final TaskRepository repository;
  private final CurrentUserService currentUserService;
  private final ProjectService projectService;
  private final LabelService labelService;
  private final Clock clock;

  /**
   * @param repository task persistence
   * @param currentUserService resolves the owning account
   * @param projectService resolves and authorises the owning project
   * @param labelService resolves and authorises attached labels
   * @param clock supplies the current date, injected so views are testable
   */
  public TaskService(TaskRepository repository, CurrentUserService currentUserService,
      ProjectService projectService, LabelService labelService, Clock clock) {
    this.repository = repository;
    this.currentUserService = currentUserService;
    this.projectService = projectService;
    this.labelService = labelService;
    this.clock = clock;
  }

  /**
   * @param query filters, sort and paging requested by the caller
   * @return matching page of the caller's own tasks
   * @throws AppException when the sort field is not sortable
   */
  public Page<TaskResponseDto> findTasks(TaskQuery query) {
    AppUser owner = currentUserService.requireCurrentUser();
    LocalDate today = LocalDate.now(clock);
    Specification<Task> specification = TaskSpecifications.ownedBy(owner.getId())
        .and(TaskSpecifications.inView(query.view(), today))
        .and(TaskSpecifications.hasStatus(query.status()))
        .and(TaskSpecifications.hasPriority(query.priority()))
        .and(TaskSpecifications.inProject(query.projectId()))
        .and(TaskSpecifications.hasLabel(query.labelId()))
        .and(TaskSpecifications.inQuadrant(query.urgent(), query.important()))
        .and(TaskSpecifications.dueBetween(query.from(), query.to()))
        .and(TaskSpecifications.titleContains(query.search()));
    PageRequest pageRequest = PageRequest.of(
        Math.max(query.page(), 0),
        Math.clamp(query.size(), 1, MAX_PAGE_SIZE),
        Sort.by(query.direction(), resolveSortField(query.sortBy()))
    );
    return repository.findAll(specification, pageRequest).map(TaskResponseDto::from);
  }

  /**
   * @param date day to summarise, {@code null} for today
   * @return completion counts for the day
   */
  public DailyProgressDto findDailyProgress(LocalDate date) {
    AppUser owner = currentUserService.requireCurrentUser();
    LocalDate target = date == null ? LocalDate.now(clock) : date;
    return DailyProgressDto.of(
        target,
        repository.countDueOn(owner.getId(), target),
        repository.countCompletedDueOn(owner.getId(), target)
    );
  }

  /**
   * @param id task identifier
   * @return the caller's task
   * @throws AppException when the task does not exist or belongs to another account
   */
  public TaskResponseDto findById(Long id) {
    return TaskResponseDto.from(requireOwnTask(id));
  }

  /**
   * @param request fields of the new task
   * @return the created task
   */
  @Transactional
  public TaskResponseDto create(TaskRequestDto request) {
    AppUser owner = currentUserService.requireCurrentUser();
    Task task = new Task(
        owner,
        request.title(),
        request.description(),
        request.status(),
        request.priority(),
        request.dueDate()
    );
    applyRequest(task, request, owner);
    return TaskResponseDto.from(repository.save(task));
  }

  /**
   * @param id task identifier
   * @param request replacement fields
   * @return the updated task
   * @throws AppException when the task does not exist or belongs to another account
   */
  @Transactional
  public TaskResponseDto update(Long id, TaskRequestDto request) {
    Task task = requireOwnTask(id);
    task.update(
        request.title(),
        request.description(),
        request.status(),
        request.priority(),
        request.dueDate(),
        request.urgent(),
        request.important(),
        request.startTime(),
        request.endTime(),
        request.alertEnabled()
    );
    applyRequest(task, request, task.getUser());
    return TaskResponseDto.from(task);
  }

  /**
   * @param id task identifier
   * @param status new lifecycle state
   * @return the updated task
   * @throws AppException when the task does not exist or belongs to another account
   */
  @Transactional
  public TaskResponseDto updateStatus(Long id, TaskStatus status) {
    Task task = requireOwnTask(id);
    task.changeStatus(status);
    return TaskResponseDto.from(task);
  }

  /**
   * Soft deletes the task, keeping the row for audit purposes.
   *
   * @param id task identifier
   * @throws AppException when the task does not exist or belongs to another account
   */
  @Transactional
  public void delete(Long id) {
    requireOwnTask(id).markDeleted();
  }

  private void applyRequest(Task task, TaskRequestDto request, AppUser owner) {
    task.update(
        request.title(),
        request.description(),
        request.status(),
        request.priority(),
        request.dueDate(),
        request.urgent(),
        request.important(),
        request.startTime(),
        request.endTime(),
        request.alertEnabled()
    );
    Project project = request.projectId() == null
        ? null
        : projectService.requireOwnProject(request.projectId());
    task.moveTo(project);
    task.replaceLabels(labelService.resolveOwned(request.labelIds(), owner));
  }

  private Task requireOwnTask(Long id) {
    AppUser owner = currentUserService.requireCurrentUser();
    return repository.findByIdAndUserId(id, owner.getId())
        .orElseThrow(() -> new AppException(AppErrorCode.RESOURCE_NOT_FOUND));
  }

  private String resolveSortField(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return DEFAULT_SORT_FIELD;
    }
    if (!SORTABLE_FIELDS.contains(sortBy)) {
      throw new AppException(AppErrorCode.INVALID_REQUEST,
          "並び替えに使用できない項目が指定されました。");
    }
    return sortBy;
  }
}
