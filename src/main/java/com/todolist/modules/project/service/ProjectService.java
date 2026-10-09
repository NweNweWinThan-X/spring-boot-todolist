package com.todolist.modules.project.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.service.CurrentUserService;
import com.todolist.modules.project.domain.Project;
import com.todolist.modules.project.domain.ProjectDeletedEvent;
import com.todolist.modules.project.dto.ProjectDto;
import com.todolist.modules.project.dto.ProjectRequestDto;
import com.todolist.modules.project.repository.ProjectRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project business logic, scoped to the authenticated account.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

  private final ProjectRepository repository;
  private final CurrentUserService currentUserService;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * @param repository project persistence
   * @param currentUserService resolves the owning account
   * @param eventPublisher announces deletions to the modules that hold references
   */
  public ProjectService(ProjectRepository repository, CurrentUserService currentUserService,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.currentUserService = currentUserService;
    this.eventPublisher = eventPublisher;
  }

  /**
   * @return the caller's projects, archived ones last
   */
  public List<ProjectDto> findAll() {
    AppUser owner = currentUserService.requireCurrentUser();
    return repository.findByUserId(owner.getId()).stream().map(ProjectDto::from).toList();
  }

  /**
   * @param request fields of the new project
   * @return the created project
   * @throws AppException when the name is already used by the account
   */
  @Transactional
  public ProjectDto create(ProjectRequestDto request) {
    AppUser owner = currentUserService.requireCurrentUser();
    requireNameAvailable(owner, request.name());
    return ProjectDto.from(
        repository.save(new Project(owner, request.name(), request.color())));
  }

  /**
   * @param id project identifier
   * @param request replacement fields
   * @return the updated project
   * @throws AppException when the project is absent, or the new name is taken
   */
  @Transactional
  public ProjectDto update(Long id, ProjectRequestDto request) {
    Project project = requireOwnProject(id);
    if (!project.getName().equalsIgnoreCase(request.name())) {
      requireNameAvailable(project.getUser(), request.name());
    }
    project.update(request.name(), request.color());
    return ProjectDto.from(project);
  }

  /**
   * @param id project identifier
   * @param archived whether the project should be hidden from the active list
   * @return the updated project
   */
  @Transactional
  public ProjectDto setArchived(Long id, boolean archived) {
    Project project = requireOwnProject(id);
    if (archived) {
      project.archive();
    } else {
      project.unarchive();
    }
    return ProjectDto.from(project);
  }

  /**
   * Soft deletes the project. Its tasks are kept and fall back to the Inbox, because losing work
   * silently is worse than leaving it unfiled.
   *
   * @param id project identifier
   */
  @Transactional
  public void delete(Long id) {
    Project project = requireOwnProject(id);
    project.markDeleted();
    eventPublisher.publishEvent(
        new ProjectDeletedEvent(project.getId(), project.getUser().getId()));
  }

  /**
   * @param id project identifier
   * @return the caller's own project entity
   * @throws AppException when the project does not exist or belongs to another account
   */
  @Transactional(readOnly = true)
  public Project requireOwnProject(Long id) {
    AppUser owner = currentUserService.requireCurrentUser();
    return repository.findByIdAndUserId(id, owner.getId())
        .orElseThrow(() -> new AppException(AppErrorCode.RESOURCE_NOT_FOUND));
  }

  private void requireNameAvailable(AppUser owner, String name) {
    if (repository.existsByUserIdAndName(owner.getId(), name)) {
      throw new AppException(AppErrorCode.CONFLICT, "同じ名前のプロジェクトが既に存在します。");
    }
  }
}
