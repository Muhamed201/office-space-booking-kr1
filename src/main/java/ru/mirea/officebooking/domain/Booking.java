package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "booking_number", nullable = false, unique = true, length = 20)
  private String bookingNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BookingStatus status;

  @Column(name = "attendees")
  private Integer attendees;

  @Column(name = "purpose", length = 500)
  private String purpose;

  @Column(name = "description", length = 2000)
  private String description;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  @Column(name = "is_recurring", nullable = false)
  private Boolean isRecurring = false;

  @Column(name = "requires_equipment", nullable = false)
  private Boolean requiresEquipment = false;

  @Column(name = "priority", nullable = false)
  private Integer priority = 1;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private Set<BookingNote> notes = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "booking_tag",
      joinColumns = @JoinColumn(name = "booking_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id")
  )
  private Set<Tag> tags = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "archived", nullable = false)
  private Boolean archived = false;

  public Booking() {}

  public Booking(String bookingNumber, Employee employee, Workspace workspace,
      LocalDateTime startTime, LocalDateTime endTime, BookingStatus status) {
    this.id = UUID.randomUUID();
    this.bookingNumber = bookingNumber;
    this.employee = employee;
    this.workspace = workspace;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.isRecurring = false;
    this.requiresEquipment = false;
    this.priority = 1;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();

    this.archived = false;
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getBookingNumber() { return bookingNumber; }
  public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
  public Employee getEmployee() { return employee; }
  public void setEmployee(Employee employee) { this.employee = employee; }
  public Workspace getWorkspace() { return workspace; }
  public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
  public BookingStatus getStatus() { return status; }
  public void setStatus(BookingStatus status) { this.status = status; }
  public Integer getAttendees() { return attendees; }
  public void setAttendees(Integer attendees) { this.attendees = attendees; }
  public String getPurpose() { return purpose; }
  public void setPurpose(String purpose) { this.purpose = purpose; }
  public Boolean getIsRecurring() { return isRecurring; }
  public void setIsRecurring(Boolean recurring) { isRecurring = recurring; }
  public Boolean getRequiresEquipment() { return requiresEquipment; }
  public void setRequiresEquipment(Boolean requiresEquipment) { this.requiresEquipment = requiresEquipment; }
  public Integer getPriority() { return priority; }
  public void setPriority(Integer priority) { this.priority = priority; }
  public Set<BookingNote> getNotes() { return notes; }
  public void setNotes(Set<BookingNote> notes) { this.notes = notes; }
  public Set<Tag> getTags() { return tags; }
  public void setTags(Set<Tag> tags) { this.tags = tags; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
  public Boolean getArchived() { return archived; }
  public void setArchived(Boolean archived) { this.archived = archived; }

  public void calculatePriority() {
    int priority = 1;
    long durationHours = java.time.Duration.between(startTime, endTime).toHours();
    if (durationHours > 2) priority += 2;
    if (attendees != null && attendees > 5) priority += 3;
    if (Boolean.TRUE.equals(requiresEquipment)) priority += 2;
    if (Boolean.TRUE.equals(isRecurring)) priority += 2;
    this.priority = Math.min(priority, 10);
  }

  public void addNote(BookingNote note) {
    this.notes.add(note);
    note.setBooking(this);
  }

  public void removeNote(BookingNote note) {
    this.notes.remove(note);
    note.setBooking(null);
  }

  public void addTag(Tag tag) {
    this.tags.add(tag);
    tag.getBookings().add(this);
  }

  public void removeTag(Tag tag) {
    this.tags.remove(tag);
    tag.getBookings().remove(this);
  }

  public void archive() {
    this.archived = true;
    this.updatedAt = LocalDateTime.now();
  }

  public void restore() {
    this.archived = false;
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Booking booking = (Booking) o;
    return Objects.equals(id, booking.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}