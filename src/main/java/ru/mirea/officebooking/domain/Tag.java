package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tag")
public class Tag {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true, length = 50)
  private String name;

  @Column(name = "color", length = 7)
  private String color;

  @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
  private Set<Booking> bookings = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Tag() {}

  public Tag(String name, String color) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.color = color;
    this.createdAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
  public Set<Booking> getBookings() { return bookings; }
  public void setBookings(Set<Booking> bookings) { this.bookings = bookings; }
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
    Tag tag = (Tag) o;
    return Objects.equals(id, tag.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}