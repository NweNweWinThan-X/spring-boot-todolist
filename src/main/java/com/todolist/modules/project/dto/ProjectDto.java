package com.todolist.modules.project.dto;

import com.todolist.modules.project.domain.Project;

/**
 * Read model for a project.
 *
 * @param id surrogate key
 * @param name display name
 * @param color hex colour used by the client
 * @param archived whether the project is hidden from the active list
 */
public record ProjectDto(Long id, String name, String color, boolean archived) {

  /**
   * @param project persisted project to project
   * @return read model built from the entity
   */
  public static ProjectDto from(Project project) {
    return new ProjectDto(project.getId(), project.getName(), project.getColor(),
        project.isArchived());
  }
}
