package com.todolist.modules.label.controller;

import com.todolist.modules.label.dto.LabelDto;
import com.todolist.modules.label.dto.LabelRequestDto;
import com.todolist.modules.label.service.LabelService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Label endpoints, scoped to the authenticated account.
 */
@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

  private final LabelService labelService;

  /**
   * @param labelService label business logic
   */
  public LabelController(LabelService labelService) {
    this.labelService = labelService;
  }

  /**
   * @return the caller's labels
   */
  @GetMapping
  public ResponseEntity<List<LabelDto>> list() {
    return ResponseEntity.ok(labelService.findAll());
  }

  /**
   * @param request fields of the new label
   * @return the created label, with its location header
   */
  @PostMapping
  public ResponseEntity<LabelDto> create(@Valid @RequestBody LabelRequestDto request) {
    LabelDto created = labelService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/labels/" + created.id())).body(created);
  }

  /**
   * @param id label identifier
   * @param request replacement fields
   * @return the updated label
   */
  @PutMapping("/{id}")
  public ResponseEntity<LabelDto> update(@PathVariable Long id,
      @Valid @RequestBody LabelRequestDto request) {
    return ResponseEntity.ok(labelService.update(id, request));
  }

  /**
   * @param id label identifier
   * @return empty 204 response
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    labelService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
