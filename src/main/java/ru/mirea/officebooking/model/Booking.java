package ru.mirea.officebooking.model;

import java.time.Duration;
import java.time.LocalDateTime;
import ru.mirea.officebooking.exception.ValidationException;

public class Booking {

    public static final int MAX_PURPOSE_LENGTH = 255;
    public static final int MAX_ATTENDEES = 1000;

    private final Long id;
    private final Long employeeId;
    private final Long workspaceId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final BookingStatus status;
    private final Integer attendees;
    private final String purpose;
    private final LocalDateTime createdAt;

    public Booking(Long id, Long employeeId, Long workspaceId, LocalDateTime startTime, LocalDateTime endTime,
                   BookingStatus status, Integer attendees, String purpose, LocalDateTime createdAt) {
        this.id = Validation.optionalPositiveId(id, "id брони");
        this.employeeId = Validation.requirePositiveId(employeeId, "id сотрудника");
        this.workspaceId = Validation.requirePositiveId(workspaceId, "id рабочего места");
        Validation.requireNotNull(startTime, "Начало брони");
        Validation.requireNotNull(endTime, "Конец брони");
        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Начало брони должно быть раньше конца: %s — %s".formatted(startTime, endTime));
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Validation.requireNotNull(status, "статус брони");
        this.attendees = validateAttendees(attendees);
        this.purpose = normalizePurpose(purpose);
        this.createdAt = createdAt;
    }

    public Booking(Long employeeId, Long workspaceId, LocalDateTime startTime, LocalDateTime endTime,
                   Integer attendees, String purpose) {
        this(null, employeeId, workspaceId, startTime, endTime, BookingStatus.ACTIVE, attendees, purpose, null);
    }

    public static String normalizePurpose(String value) {
        return Validation.optionalText(value, "Цель брони", MAX_PURPOSE_LENGTH);
    }

    public static Integer validateAttendees(Integer value) {
        if (value == null) {
            return null;
        }
        Validation.requireInRange(value, 1, MAX_ATTENDEES, "Число участников");
        return value;
    }

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    public long durationHours() {
        return duration().toHours();
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public boolean overlaps(Booking other) {
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public Long getId() {
        return id;
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

    public Booking withId(long newId) {
        return new Booking(newId, employeeId, workspaceId, startTime, endTime, status, attendees, purpose, createdAt);
    }

    public Booking withStatus(BookingStatus newStatus) {
        return new Booking(id, employeeId, workspaceId, startTime, endTime, newStatus, attendees, purpose, createdAt);
    }

    public Booking withDetails(LocalDateTime newStart, LocalDateTime newEnd, Integer newAttendees, String newPurpose) {
        return new Booking(id, employeeId, workspaceId, newStart, newEnd, status, newAttendees, newPurpose, createdAt);
    }

    @Override
    public String toString() {
        return "#%d сотрудник=%d место=%d [%s — %s] %s%s%s".formatted(
                id, employeeId, workspaceId, startTime, endTime, status,
                attendees == null ? "" : ", участников: " + attendees,
                purpose == null ? "" : ", цель: " + purpose);
    }
}
