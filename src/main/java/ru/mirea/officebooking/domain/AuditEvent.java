package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 20)
  private EventType eventType;

  @Column(name = "entity_type", length = 50)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public AuditEvent() {}

  public AuditEvent(EventType eventType, String description) {
    this.id = UUID.randomUUID();
    this.eventType = eventType;
    this.description = description;
    this.createdAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UserAccount getUser() { return user; }
  public void setUser(UserAccount user) { this.user = user; }
  public EventType getEventType() { return eventType; }
  public void setEventType(EventType eventType) { this.eventType = eventType; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public UUID getEntityId() { return entityId; }
  public void setEntityId(UUID entityId) { this.entityId = entityId; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCorrelationId() { return correlationId; }
  public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuditEvent that = (AuditEvent) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}