package ru.mirea.officebooking.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.BookingNote;
import ru.mirea.officebooking.domain.EventType;
import ru.mirea.officebooking.domain.Role;
import ru.mirea.officebooking.error.EntityNotFoundException;
import ru.mirea.officebooking.repository.BookingNoteRepository;
import ru.mirea.officebooking.repository.UserAccountRepository;
import ru.mirea.officebooking.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingNoteService {
    public record Request(@NotBlank @Size(max = 2000) @Pattern(regexp = "[\\P{Cc}\\t\\r\\n]*") String content, Boolean isPinned) {}
    public record Note(UUID id, UUID bookingId, UUID authorId, String authorName, String content,
                       boolean isPinned, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    private final BookingService bookings;
    private final BookingNoteRepository notes;
    private final UserAccountRepository users;
    private final AuditService audit;

    public BookingNoteService(BookingService bookings, BookingNoteRepository notes,
                              UserAccountRepository users, AuditService audit) {
        this.bookings = bookings;
        this.notes = notes;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Note> list(UUID bookingId) {
        bookings.visibleBooking(bookingId);
        return notes.findByBookingIdOrderByIsPinnedDescCreatedAtAscIdAsc(bookingId).stream().map(this::dto).toList();
    }

    public Note create(UUID bookingId, Request request) {
        var booking = bookings.visibleBooking(bookingId);
        var author = users.getReferenceById(CurrentUser.get().id());
        var note = new BookingNote(booking, author, request.content().strip());
        note.setIsPinned(Boolean.TRUE.equals(request.isPinned()));
        note = notes.saveAndFlush(note);
        audit.logEvent(EventType.CREATE, "BookingNote", note.getId(), "Добавлена заметка");
        return dto(note);
    }

    public Note update(UUID id, Request request) {
        BookingNote note = editable(id);
        note.setContent(request.content().strip());
        if (request.isPinned() != null) {
            note.setIsPinned(request.isPinned());
        }
        notes.flush();
        audit.logEvent(EventType.UPDATE, "BookingNote", id, "Обновлена заметка");
        return dto(note);
    }

    public void delete(UUID id) {
        notes.delete(editable(id));
        audit.logEvent(EventType.DELETE, "BookingNote", id, "Удалена заметка");
    }

    private BookingNote editable(UUID id) {
        BookingNote note = notes.findById(id).orElseThrow(() -> new EntityNotFoundException("Заметка не найдена"));
        bookings.visibleBooking(note.getBooking().getId());
        var user = CurrentUser.get();
        if (user.role() != Role.ADMIN && !note.getAuthor().getId().equals(user.id())) {
            throw new AccessDeniedException("Редактирование доступно автору и администратору");
        }
        return note;
    }

    private Note dto(BookingNote note) {
        return new Note(note.getId(), note.getBooking().getId(), note.getAuthor().getId(), note.getAuthor().getName(),
                note.getContent(), note.getIsPinned(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
