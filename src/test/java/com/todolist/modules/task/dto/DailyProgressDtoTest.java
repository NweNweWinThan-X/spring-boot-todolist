package com.todolist.modules.task.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DailyProgressDtoTest {

  private static final LocalDate DATE = LocalDate.of(2026, 10, 10);

  @Test
  @DisplayName("0件の日は0除算にならず0%を返す")
  void treatsAnEmptyDayAsZeroPercent() {
    assertThat(DailyProgressDto.of(DATE, 0, 0).percentage()).isZero();
  }

  @ParameterizedTest(name = "{1}/{0} -> {2}%")
  @CsvSource({
      "3, 2, 67",
      "2, 1, 50",
      "4, 4, 100",
      "7, 0, 0",
      "3, 1, 33",
  })
  void roundsThePercentageToTheNearestWholeNumber(long total, long completed, int expected) {
    assertThat(DailyProgressDto.of(DATE, total, completed).percentage()).isEqualTo(expected);
  }
}
