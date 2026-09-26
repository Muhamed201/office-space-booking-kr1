package ru.mirea.officebooking.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITION = new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITION.put(ACTIVE, EnumSet.of(CANCELLED, COMPLETED, NO_SHOW));
        ALLOWED_TRANSITION.put(CANCELLED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITION.put(COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITION.put(NO_SHOW, EnumSet.noneOf(BookingStatus.class));
    }

    public boolean canTransitionTo(BookingStatus bookingStatus) {
        return ALLOWED_TRANSITION.get(this).contains(bookingStatus);
    }

    public boolean isFinal() {
        return ALLOWED_TRANSITION.get(this).isEmpty();
    }
}
