package ru.mirea.officebooking.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public record BookingRow(
        long id,
        long employeeId,
        String employeeName,
        String employeeEmail,
        long workspaceId,
        String workspaceCode,
        WorkspaceType workspaceType,
        int floor,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        Integer attendees,
        String purpose) {

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }
}
