package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "booking_note")
public class BookingNote {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private UserAccount author;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "is_pinned", nullable = false)
  private Boolean isPinned = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public BookingNote() {}

  public BookingNote(Booking booking, UserAccount author, String content) {
    this.id = UUID.randomUUID();
    this.booking = booking;
    this.author = author;
    this.content = content;
    this.isPinned = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Booking getBooking() { return booking; }
  public void setBooking(Booking booking) { this.booking = booking; }
  public UserAccount getAuthor() { return author; }
  public void setAuthor(UserAccount author) { this.author = author; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Boolean getIsPinned() { return isPinned; }
  public void setIsPinned(Boolean pinned) { isPinned = pinned; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public void pin() {
    this.isPinned = true;
    this.updatedAt = LocalDateTime.now();
  }

  public void unpin() {
    this.isPinned = false;
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
    BookingNote that = (BookingNote) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}