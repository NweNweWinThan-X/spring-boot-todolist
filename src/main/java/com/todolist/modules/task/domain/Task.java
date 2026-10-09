package com.todolist.modules.task.domain;

import com.todolist.modules.auth.domain.AppUser;
import com.todolist.modules.label.domain.Label;
import com.todolist.modules.project.domain.Project;
import com.todolist.shared.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

/**
 * A single to-do item belonging to one account.
 *
 * <p>A task sits in at most one project; with none it belongs to the Inbox. Labels are free-form
 * metadata and may be attached many to many.
 */
@Entity
@Table(name = "task")
@SequenceGenerator(name = "task_seq", sequenceName = "task_seq", allocationSize = 50)
@Getter
public class Task extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
  private Long id;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TaskStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 20)
  private TaskPriority priority;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "alert_enabled", nullable = false)
  private boolean alertEnabled = false;

  @Column(name = "urgent", nullable = false)
  private boolean urgent = false;

  @Column(name = "important", nullable = false)
  private boolean important = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  /*
   * Set rather than List, and no REMOVE cascade: detaching a task must never delete the label,
   * which other tasks still reference.
   */
  @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "task_label",
      joinColumns = @JoinColumn(name = "task_id"),
      inverseJoinColumns = @JoinColumn(name = "label_id"))
  private Set<Label> labels = new LinkedHashSet<>();

  /**
   * Required by JPA.
   */
  protected Task() {
  }

  /**
   * @param owner account the task belongs to
   * @param title short summary
   * @param description optional detail
   * @param status initial lifecycle state
   * @param priority initial urgency ranking
   * @param dueDate optional deadline
   */
  public Task(AppUser owner, String title, String description, TaskStatus status,
      TaskPriority priority, LocalDate dueDate) {
    this.user = owner;
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.dueDate = dueDate;
  }

  /**
   * Replaces every caller editable scalar field in one step.
   *
   * @param title short summary
   * @param description optional detail
   * @param status lifecycle state
   * @param priority urgency ranking
   * @param dueDate optional deadline
   * @param urgent whether the task is time critical
   * @param important whether the task advances a goal
   * @param startTime optional start of the time box
   * @param endTime optional end of the time box
   * @param alertEnabled whether the owner asked to be reminded
   */
  public void update(String title, String description, TaskStatus status, TaskPriority priority,
      LocalDate dueDate, boolean urgent, boolean important, LocalTime startTime,
      LocalTime endTime, boolean alertEnabled) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.dueDate = dueDate;
    this.urgent = urgent;
    this.important = important;
    this.startTime = startTime;
    this.endTime = endTime;
    this.alertEnabled = alertEnabled;
  }

  /**
   * @param startTime optional start of the time box
   * @param endTime optional end of the time box
   */
  public void schedule(LocalTime startTime, LocalTime endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * @param status new lifecycle state
   */
  public void changeStatus(TaskStatus status) {
    this.status = status;
  }

  /**
   * @param project owning project, or {@code null} to move the task to the Inbox
   */
  public void moveTo(Project project) {
    this.project = project;
  }

  /**
   * @param labels labels that should be attached after the call
   */
  public void replaceLabels(Set<Label> labels) {
    this.labels.clear();
    if (labels != null) {
      this.labels.addAll(labels);
    }
  }

  /**
   * @return quadrant the urgency and importance axes place this task in
   */
  public EisenhowerQuadrant getQuadrant() {
    return EisenhowerQuadrant.of(urgent, important);
  }

  /**
   * @param candidate account to compare against the owner
   * @return whether the given account owns this task
   */
  public boolean isOwnedBy(AppUser candidate) {
    return candidate != null && user != null && user.getId() != null
        && user.getId().equals(candidate.getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Task other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
