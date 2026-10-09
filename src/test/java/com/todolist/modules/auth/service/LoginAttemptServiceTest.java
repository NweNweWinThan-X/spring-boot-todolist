package com.todolist.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the brute force threshold required by security audit No.9.
 */
class LoginAttemptServiceTest {

  private static final String USERNAME = "tester";
  private static final int MAX_ATTEMPTS = 5;

  @Test
  @DisplayName("失敗が閾値未満ならブロックされない")
  void doesNotBlockBelowThreshold() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
      service.recordFailure(USERNAME);
    }
    assertThat(service.isBlocked(USERNAME)).isFalse();
  }

  @Test
  @DisplayName("失敗が閾値に達するとブロックされる")
  void blocksAtThreshold() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      service.recordFailure(USERNAME);
    }
    assertThat(service.isBlocked(USERNAME)).isTrue();
  }

  @Test
  @DisplayName("リセット後はブロックが解除される")
  void unblocksAfterReset() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      service.recordFailure(USERNAME);
    }
    service.reset(USERNAME);
    assertThat(service.isBlocked(USERNAME)).isFalse();
  }
}
