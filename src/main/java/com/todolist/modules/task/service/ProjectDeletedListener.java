package com.todolist.modules.task.service;

import com.todolist.modules.project.domain.ProjectDeletedEvent;
import com.todolist.modules.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Moves a deleted project's tasks back to the Inbox.
 *
 * <p>Losing a project must not lose the work inside it, so the tasks are detached rather than
 * cascaded away. Runs before commit so the detach shares the deletion's transaction.
 */
@Component
public class ProjectDeletedListener {

  private static final Logger log = LoggerFactory.getLogger(ProjectDeletedListener.class);

  private final TaskRepository repository;

  /**
   * @param repository task persistence
   */
  public ProjectDeletedListener(TaskRepository repository) {
    this.repository = repository;
  }

  /**
   * @param event project that was removed
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  @Transactional(propagation = Propagation.MANDATORY)
  public void onProjectDeleted(ProjectDeletedEvent event) {
    int moved = repository.detachFromProject(event.projectId(), event.userId());
    log.info("プロジェクト削除に伴いタスクを受信箱へ移動: projectId={}, moved={}",
        event.projectId(), moved);
  }
}
