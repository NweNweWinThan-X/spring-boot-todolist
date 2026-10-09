package com.todolist.modules.label.domain;

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
 * Reusable metadata attached to tasks.
 *
 * <p>The owning side of the task association is {@code Task}, so the task collection is not
 * mapped here.
 */
@Entity
@Table(name = "label")
@SequenceGenerator(name = "label_seq", sequenceName = "label_seq", allocationSize = 50)
@Getter
public class Label extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "label_seq")
  private Long id;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "color", nullable = false, length = 7)
  private String color;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  /**
   * Required by JPA.
   */
  protected Label() {
  }

  /**
   * @param owner account the label belongs to
   * @param name display name, unique per account
   * @param color hex colour used by the client
   */
  public Label(AppUser owner, String name, String color) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Label other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
