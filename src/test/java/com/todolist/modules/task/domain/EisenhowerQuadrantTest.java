package com.todolist.modules.task.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The quadrant is derived, never stored, so this is the whole truth table.
 */
class EisenhowerQuadrantTest {

  @ParameterizedTest(name = "urgent={0}, important={1} -> {2}")
  @CsvSource({
      "true,  true,  DO",
      "false, true,  SCHEDULE",
      "true,  false, DELEGATE",
      "false, false, ELIMINATE",
  })
  void mapsBothAxesToAQuadrant(boolean urgent, boolean important, EisenhowerQuadrant expected) {
    assertThat(EisenhowerQuadrant.of(urgent, important)).isEqualTo(expected);
  }
}
