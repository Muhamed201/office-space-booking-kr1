package ru.mirea.officebooking.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Booking {

    private Long id;
    private Long employeeId;
    private Long workspaceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private Integer attendees;
    private String purpose;
    private LocalDateTime createdAt;

  
    public Booking(Long employeeId, Long workspaceId, LocalDateTime startTime, LocalDateTime endTime,
                   Integer attendees, String purpose) {
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId не может быть null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId не может быть null");
        this.startTime = Objects.requireNonNull(startTime, "startTime не может быть null");
        this.endTime = Objects.requireNonNull(endTime, "endTime не может быть null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime должен быть раньше endTime");
        }
        this.attendees = attendees;
        this.purpose = purpose;
        this.status = BookingStatus.ACTIVE;
    }

    
    public Booking(Long id, Long employeeId, Long workspaceId, LocalDateTime startTime, LocalDateTime endTime,
                   BookingStatus status, Integer attendees, String purpose, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.workspaceId = workspaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.attendees = attendees;
        this.purpose = purpose;
        this.createdAt = createdAt;
    }

    public void changeStatus(BookingStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus не может быть null");
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса брони: " + this.status + " -> " + newStatus);
        }
        this.status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Integer getAttendees() {
        return attendees;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

   
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return this.startTime.isBefore(otherEnd) && otherStart.isBefore(this.endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Booking{id=%s, employeeId=%s, workspaceId=%s, startTime=%s, endTime=%s, status=%s}"
                .formatted(id, employeeId, workspaceId, startTime, endTime, status);
    }
}
