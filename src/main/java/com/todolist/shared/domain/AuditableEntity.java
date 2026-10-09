package com.todolist.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Audit metadata shared by every table, plus a soft delete flag.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @CreatedBy
  @Column(name = "created_by", length = 50, updatable = false)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "updated_by", length = 50)
  private String updatedBy;

  @Column(name = "deleted", nullable = false)
  private boolean deleted = false;

  /**
   * @return instant the row was first persisted
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * @return instant the row was last modified
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @return username that created the row, or {@code null} for system writes
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * @return username that last modified the row, or {@code null} for system writes
   */
  public String getUpdatedBy() {
    return updatedBy;
  }

  /**
   * @return whether the row is soft deleted and must be excluded from queries
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * Marks the row as soft deleted. Rows are never removed physically.
   */
  public void markDeleted() {
    this.deleted = true;
  }
}
