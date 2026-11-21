package ru.mirea.officebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.officebooking.domain.Booking;
import ru.mirea.officebooking.domain.BookingStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
    @Query(value = "select nextval('booking_number_seq')", nativeQuery = true)
    long nextBookingNumber();

    Optional<Booking> findByBookingNumber(String bookingNumber);
    boolean existsByWorkspaceIdAndArchivedFalseAndStatusAndEndTimeAfter(
            UUID workspaceId, BookingStatus status, LocalDateTime now);
    boolean existsByWorkspaceIdAndArchivedFalseAndStatusAndEndTimeAfterAndAttendeesGreaterThan(
            UUID workspaceId, BookingStatus status, LocalDateTime now, int capacity);
}
