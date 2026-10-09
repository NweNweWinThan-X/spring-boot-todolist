package com.todolist.modules.project.controller;

import com.todolist.modules.project.dto.ProjectDto;
import com.todolist.modules.project.dto.ProjectRequestDto;
import com.todolist.modules.project.service.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project endpoints, scoped to the authenticated account.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

  private final ProjectService projectService;

  /**
   * @param projectService project business logic
   */
  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  /**
   * @return the caller's projects
   */
  @GetMapping
  public ResponseEntity<List<ProjectDto>> list() {
    return ResponseEntity.ok(projectService.findAll());
  }

  /**
   * @param request fields of the new project
   * @return the created project, with its location header
   */
  @PostMapping
  public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectRequestDto request) {
    ProjectDto created = projectService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/projects/" + created.id())).body(created);
  }

  /**
   * @param id project identifier
   * @param request replacement fields
   * @return the updated project
   */
  @PutMapping("/{id}")
  public ResponseEntity<ProjectDto> update(@PathVariable Long id,
      @Valid @RequestBody ProjectRequestDto request) {
    return ResponseEntity.ok(projectService.update(id, request));
  }

  /**
   * @param id project identifier
   * @param archived whether the project should be hidden from the active list
   * @return the updated project
   */
  @PatchMapping("/{id}/archived")
  public ResponseEntity<ProjectDto> setArchived(@PathVariable Long id,
      @RequestParam boolean archived) {
    return ResponseEntity.ok(projectService.setArchived(id, archived));
  }

  /**
   * @param id project identifier
   * @return empty 204 response
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    projectService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
