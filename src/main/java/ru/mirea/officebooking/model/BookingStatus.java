package ru.mirea.officebooking.model;


public enum BookingStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    public boolean canTransitionTo(BookingStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case ACTIVE -> target == CANCELLED || target == COMPLETED || target == NO_SHOW;
            case CANCELLED, COMPLETED, NO_SHOW -> false;
        };
    }
}
