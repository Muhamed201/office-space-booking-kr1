package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workspace")
public class Workspace {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "code", nullable = false, unique = true, length = 20)
  private String code;

  @Column(name = "floor", nullable = false)
  private Integer floor;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private WorkspaceType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private WorkspaceStatus status;

  @Column(name = "has_monitor", nullable = false)
  private Boolean hasMonitor = false;

  @Column(name = "capacity", nullable = false)
  private Integer capacity = 1;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public Workspace() {}

  public Workspace(String code, Integer floor, WorkspaceType type, WorkspaceStatus status) {
    this.id = UUID.randomUUID();
    this.code = code;
    this.floor = floor;
    this.type = type;
    this.status = status;
    this.hasMonitor = false;
    this.capacity = 1;
    this.enabled = true;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public Integer getFloor() { return floor; }
  public void setFloor(Integer floor) { this.floor = floor; }
  public WorkspaceType getType() { return type; }
  public void setType(WorkspaceType type) { this.type = type; }
  public WorkspaceStatus getStatus() { return status; }
  public void setStatus(WorkspaceStatus status) { this.status = status; }
  public Boolean getHasMonitor() { return hasMonitor; }
  public void setHasMonitor(Boolean hasMonitor) { this.hasMonitor = hasMonitor; }
  public Integer getCapacity() { return capacity; }
  public void setCapacity(Integer capacity) { this.capacity = capacity; }
  public Boolean getEnabled() { return enabled; }
  public void setEnabled(Boolean enabled) { this.enabled = enabled; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public void enable() {
    this.enabled = true;
    this.updatedAt = LocalDateTime.now();
  }

  public void disable() {
    this.enabled = false;
    this.updatedAt = LocalDateTime.now();
  }

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Workspace workspace = (Workspace) o;
    return Objects.equals(id, workspace.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}