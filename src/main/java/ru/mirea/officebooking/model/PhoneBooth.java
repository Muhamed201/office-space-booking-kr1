package ru.mirea.officebooking.model;

import ru.mirea.officebooking.exception.ValidationException;

public class PhoneBooth extends Workspace {

    private static final int MAX_BOOKING_HOURS = 1;

    public PhoneBooth(Long id, String code, int floor, WorkspaceStatus status, boolean hasMonitor) {
        super(id, code, floor, status, hasMonitor);
    }

    @Override
    public WorkspaceType type() {
        return WorkspaceType.PHONE_BOOTH;
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
        return new PhoneBooth(newId, getCode(), getFloor(), getStatus(), isHasMonitor());
    }

    @Override
    public Workspace withStatus(WorkspaceStatus newStatus) {
        return new PhoneBooth(getId(), getCode(), getFloor(), newStatus, isHasMonitor());
    }

    @Override
    public Workspace withDetails(String newCode, int newFloor, boolean newHasMonitor) {
        return new PhoneBooth(getId(), newCode, newFloor, getStatus(), newHasMonitor);
    }
}
