package com.todolist.modules.project.domain;

/**
 * Published when a project is soft deleted.
 *
 * <p>Other modules react to this instead of the project module reaching into them, which keeps
 * the module boundary one-way.
 *
 * @param projectId project that was removed
 * @param userId account the project belonged to
 */
public record ProjectDeletedEvent(Long projectId, Long userId) {
}
