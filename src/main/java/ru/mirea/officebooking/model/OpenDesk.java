package ru.mirea.officebooking.model;

import ru.mirea.officebooking.exception.ValidationException;

public class OpenDesk extends Workspace {

    private static final int MAX_BOOKING_HOURS = 10;

    public OpenDesk(Long id, String code, int floor, WorkspaceStatus status, boolean hasMonitor) {
        super(id, code, floor, status, hasMonitor);
    }

    @Override
    public WorkspaceType type() {
        return WorkspaceType.OPEN_DESK;
    }

    @Override
    public int maxBookingHours() {
        return MAX_BOOKING_HOURS;
    }

    @Override
    public void validateBooking(Booking booking) {
        if (booking.getAttendees() != null) {
            throw new ValidationException("Число участников указывается только для переговорной, а не для места " + getCode());
        }
    }

    @Override
    public Workspace withId(long newId) {
        return new OpenDesk(newId, getCode(), getFloor(), getStatus(), isHasMonitor());
    }

    @Override
    public Workspace withStatus(WorkspaceStatus newStatus) {
        return new OpenDesk(getId(), getCode(), getFloor(), newStatus, isHasMonitor());
    }

    @Override
    public Workspace withDetails(String newCode, int newFloor, boolean newHasMonitor) {
        return new OpenDesk(getId(), newCode, newFloor, getStatus(), newHasMonitor);
    }
}
