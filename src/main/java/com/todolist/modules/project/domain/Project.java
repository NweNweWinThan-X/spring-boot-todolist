package com.todolist.modules.project.domain;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A non-overlapping area of life that groups tasks.
 *
 * <p>A task belongs to at most one project; a task with none is in the Inbox. The task collection
 * is deliberately not mapped here — it can grow without bound, so children are queried directly.
 */
@Entity
@Table(name = "project")
@SequenceGenerator(name = "project_seq", sequenceName = "project_seq", allocationSize = 50)
@Getter
public class Project extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq")
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "color", nullable = false, length = 7)
  private String color;

  @Column(name = "archived", nullable = false)
  private boolean archived = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  /**
   * Required by JPA.
   */
  protected Project() {
  }

  /**
   * @param owner account the project belongs to
   * @param name display name, unique per account
   * @param color hex colour used by the client
   */
  public Project(AppUser owner, String name, String color) {
    this.user = owner;
    this.name = name;
    this.color = color;
  }

  /**
   * @param name new display name
   * @param color new hex colour
   */
  public void update(String name, String color) {
    this.name = name;
    this.color = color;
  }

  /**
   * Hides the project without deleting it or its tasks.
   */
  public void archive() {
    this.archived = true;
  }

  /**
   * Returns an archived project to the active list.
   */
  public void unarchive() {
    this.archived = false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Project other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
