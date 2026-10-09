package com.todolist.modules.label.dto;

import com.todolist.modules.label.domain.Label;

/**
 * Read model for a label.
 *
 * @param id surrogate key
 * @param name display name
 * @param color hex colour used by the client
 */
public record LabelDto(Long id, String name, String color) {

  /**
   * @param label persisted label to project
   * @return read model built from the entity
   */
  public static LabelDto from(Label label) {
    return new LabelDto(label.getId(), label.getName(), label.getColor());
  }
}
