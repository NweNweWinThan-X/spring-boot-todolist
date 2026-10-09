package com.todolist.modules.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.auth.domain.Role;
import com.todolist.modules.auth.service.CurrentUserService;
import com.todolist.modules.label.service.LabelService;
import com.todolist.modules.project.service.ProjectService;
import com.todolist.modules.task.domain.Task;
import com.todolist.modules.task.domain.TaskPriority;
import com.todolist.modules.task.domain.TaskStatus;
import com.todolist.modules.task.dto.TaskQuery;
import com.todolist.modules.task.dto.TaskRequestDto;
import com.todolist.modules.task.repository.TaskRepository;
import com.todolist.shared.exception.AppErrorCode;
import com.todolist.shared.exception.AppException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for the rules that do not need a database: owner scoping, the sort allowlist,
 * page-size clamping and soft delete.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final Clock FIXED =
      Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneId.of("Asia/Tokyo"));

  @Mock
  private TaskRepository repository;

  @Mock
  private CurrentUserService currentUserService;

  @Mock
  private ProjectService projectService;

  @Mock
  private LabelService labelService;

  private TaskService service;
  private AppUser owner;

  private static void setId(Object target, Long id) throws Exception {
    Field field = target.getClass().getDeclaredField("id");
    field.setAccessible(true);
    field.set(target, id);
  }

  private static TaskQuery query(String sortBy) {
    return new TaskQuery(null, null, null, null, null, null, null, null, null, null, sortBy,
        Sort.Direction.DESC, 0, 20);
  }

  private static TaskRequestDto request() {
    return new TaskRequestDto("Title", null, TaskStatus.PENDING, TaskPriority.LOW, null,
        false, false, null, null, false, null, Set.of());
  }

  @BeforeEach
  void setUp() throws Exception {
    service = new TaskService(repository, currentUserService, projectService, labelService, FIXED);
    owner = new AppUser("alice", "alice@example.com", "hash", "Alice", Role.USER);
    setId(owner, 7L);
  }

  @Test
  @DisplayName("並び替え項目が許可リストにない場合は400相当で失敗する")
  void rejectsASortFieldOutsideTheAllowlist() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);

    assertThatThrownBy(() -> service.findTasks(query("user.passwordHash")))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(AppErrorCode.INVALID_REQUEST);

    verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  @DisplayName("許可された項目は並び替えに使える")
  void acceptsAnAllowlistedSortField() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());

    assertThat(service.findTasks(query("dueDate"))).isEmpty();
  }

  @Test
  @DisplayName("ページサイズは上限100に丸められる")
  void clampsThePageSize() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenAnswer(invocation -> {
          Pageable pageable = invocation.getArgument(1);
          assertThat(pageable.getPageSize()).isEqualTo(100);
          assertThat(pageable.getPageNumber()).isZero();
          return Page.empty();
        });

    service.findTasks(new TaskQuery(null, null, null, null, null, null, null, null, null, null,
        null, Sort.Direction.DESC, -5, 5_000));
  }

  @Test
  @DisplayName("他人のタスクは404相当で見えない")
  void hidesAnotherAccountsTask() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    when(repository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(99L))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(AppErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("削除は論理削除で、行は残る")
  void deletesSoftly() {
    Task task = new Task(owner, "Temp", null, TaskStatus.PENDING, TaskPriority.LOW, null);
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    when(repository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(task));

    service.delete(1L);

    assertThat(task.isDeleted()).isTrue();
    // delete(..) is overloaded on JpaSpecificationExecutor, so disambiguate with the entity type.
    verify(repository, never()).delete(any(Task.class));
    verify(repository, never()).deleteById(any());
  }

  @Test
  @DisplayName("作成時のプロジェクトとラベルは所有者で検証される")
  void resolvesProjectAndLabelsThroughTheOwningServices() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    when(labelService.resolveOwned(any(), eq(owner))).thenReturn(Set.of());
    when(repository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

    service.create(request());

    verify(labelService).resolveOwned(Set.of(), owner);
    verify(projectService, never()).requireOwnProject(any());
  }

  @Test
  @DisplayName("進捗はClockの「今日」を使い、0件でも0除算しない")
  void summarisesTheDayFromTheInjectedClock() {
    when(currentUserService.requireCurrentUser()).thenReturn(owner);
    LocalDate today = LocalDate.now(FIXED);
    when(repository.countDueOn(7L, today)).thenReturn(0L);
    when(repository.countCompletedDueOn(7L, today)).thenReturn(0L);

    assertThat(service.findDailyProgress(null).percentage()).isZero();
    assertThat(service.findDailyProgress(null).date()).isEqualTo(today);
  }
}
