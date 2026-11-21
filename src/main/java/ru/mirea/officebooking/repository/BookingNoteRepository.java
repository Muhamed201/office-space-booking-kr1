package ru.mirea.officebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.officebooking.domain.BookingNote;

import java.util.List;
import java.util.UUID;

public interface BookingNoteRepository extends JpaRepository<BookingNote, UUID> {
    List<BookingNote> findByBookingIdOrderByIsPinnedDescCreatedAtAscIdAsc(UUID bookingId);
}
