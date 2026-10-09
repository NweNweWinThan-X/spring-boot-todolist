package com.todolist.modules.label.service;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.service.CurrentUserService;
import com.todolist.modules.label.domain.Label;
import com.todolist.modules.label.dto.LabelDto;
import com.todolist.modules.label.dto.LabelRequestDto;
import com.todolist.modules.label.repository.LabelRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Label business logic, scoped to the authenticated account.
 */
@Service
@Transactional(readOnly = true)
public class LabelService {

  private final LabelRepository repository;
  private final CurrentUserService currentUserService;

  /**
   * @param repository label persistence
   * @param currentUserService resolves the owning account
   */
  public LabelService(LabelRepository repository, CurrentUserService currentUserService) {
    this.repository = repository;
    this.currentUserService = currentUserService;
  }

  /**
   * @return the caller's labels ordered by name
   */
  public List<LabelDto> findAll() {
    AppUser owner = currentUserService.requireCurrentUser();
    return repository.findByUserId(owner.getId()).stream().map(LabelDto::from).toList();
  }

  /**
   * @param request fields of the new label
   * @return the created label
   * @throws AppException when the name is already used by the account
   */
  @Transactional
  public LabelDto create(LabelRequestDto request) {
    AppUser owner = currentUserService.requireCurrentUser();
    requireNameAvailable(owner, request.name());
    return LabelDto.from(repository.save(new Label(owner, request.name(), request.color())));
  }

  /**
   * @param id label identifier
   * @param request replacement fields
   * @return the updated label
   * @throws AppException when the label is absent, or the new name is taken
   */
  @Transactional
  public LabelDto update(Long id, LabelRequestDto request) {
    Label label = requireOwnLabel(id);
    if (!label.getName().equalsIgnoreCase(request.name())) {
      requireNameAvailable(label.getUser(), request.name());
    }
    label.update(request.name(), request.color());
    return LabelDto.from(label);
  }

  /**
   * Soft deletes the label. Rows in {@code task_label} are left alone; the label simply stops
   * being resolved for the tasks that referenced it.
   *
   * @param id label identifier
   */
  @Transactional
  public void delete(Long id) {
    requireOwnLabel(id).markDeleted();
  }

  /**
   * @param ids label identifiers requested by the caller
   * @param owner account the labels must belong to
   * @return the resolved labels
   * @throws AppException when any identifier does not resolve to one of the account's labels
   */
  @Transactional(readOnly = true)
  public Set<Label> resolveOwned(Set<Long> ids, AppUser owner) {
    if (ids == null || ids.isEmpty()) {
      return Set.of();
    }
    Set<Label> labels = repository.findAllByIdsAndUserId(ids, owner.getId());
    if (labels.size() != ids.size()) {
      throw new AppException(AppErrorCode.RESOURCE_NOT_FOUND, "指定されたラベルが見つかりません。");
    }
    return labels;
  }

  private Label requireOwnLabel(Long id) {
    AppUser owner = currentUserService.requireCurrentUser();
    return repository.findByIdAndUserId(id, owner.getId())
        .orElseThrow(() -> new AppException(AppErrorCode.RESOURCE_NOT_FOUND));
  }

  private void requireNameAvailable(AppUser owner, String name) {
    if (repository.existsByUserIdAndName(owner.getId(), name)) {
      throw new AppException(AppErrorCode.CONFLICT, "同じ名前のラベルが既に存在します。");
    }
  }
}
