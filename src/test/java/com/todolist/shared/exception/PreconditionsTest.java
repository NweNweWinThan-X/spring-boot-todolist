package com.todolist.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guard clauses must raise AppException, never a JDK runtime exception.
 */
class PreconditionsTest {

  @Test
  @DisplayName("nullはAppExceptionになる")
  void rejectsNull() {
    assertThatThrownBy(() -> Preconditions.requireNonNull(null, AppErrorCode.INVALID_REQUEST))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(AppErrorCode.INVALID_REQUEST);
  }

  @Test
  @DisplayName("空白のみの文字列は拒否される")
  void rejectsBlank() {
    assertThatThrownBy(() -> Preconditions.requireNotBlank("   ", AppErrorCode.INVALID_REQUEST))
        .isInstanceOf(AppException.class);
  }

  @Test
  @DisplayName("値がある場合はそのまま返す")
  void passesThroughAValue() {
    assertThat(Preconditions.requireNotBlank("ok", AppErrorCode.INVALID_REQUEST)).isEqualTo("ok");
    assertThat(Preconditions.requireNotEmpty(List.of(1), AppErrorCode.INVALID_REQUEST))
        .containsExactly(1);
    assertThatCode(() -> Preconditions.checkState(true, AppErrorCode.CONFLICT))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("条件を満たさない場合は指定したエラーコードで失敗する")
  void failsCheckStateWithTheGivenCode() {
    assertThatThrownBy(() -> Preconditions.checkState(false, AppErrorCode.CONFLICT))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(AppErrorCode.CONFLICT);
  }
}
