package ru.mirea.officebooking.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.officebooking.domain.BookingStatus;
import ru.mirea.officebooking.dto.BulkResult;
import ru.mirea.officebooking.error.ConflictException;
import ru.mirea.officebooking.error.EntityNotFoundException;
import ru.mirea.officebooking.error.ValidationException;

import java.util.*;

@Service
public class BulkBookingService {
    public enum Operation { ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG }

    public record Request(@NotEmpty List<@NotNull UUID> ids, @NotNull Operation operation,
                          BookingStatus status, @Size(max = 50) String tag) {}

    private final BookingService booking;
    private final TransactionTemplate transaction;

    public BulkBookingService(BookingService booking, TransactionTemplate transaction) {
        this.booking = booking;
        this.transaction = transaction;
    }

    public BulkResult apply(Request request) {
        booking.requireManager();
        if (request.operation() == Operation.CHANGE_STATUS && request.status() == null) {
            throw new ValidationException("Выберите статус");
        }
        if ((request.operation() == Operation.ADD_TAG || request.operation() == Operation.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new ValidationException("Укажите тег");
        }
        List<String> errors = new ArrayList<>();
        Set<UUID> ids = new LinkedHashSet<>(request.ids());
        for (UUID id : ids) {
            try {
                transaction.executeWithoutResult(status -> applyToOne(id, request));
            } catch (ValidationException | EntityNotFoundException | ConflictException e) {
                errors.add(id + ": " + e.getMessage());
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                errors.add(id + ": конфликт с существующей записью");
            }
        }
        return new BulkResult(ids.size() - errors.size(), errors.size(), errors);
    }

    private void applyToOne(UUID id, Request request) {
        switch (request.operation()) {
            case ARCHIVE -> booking.archiveBooking(id);
            case RESTORE -> booking.restoreBooking(id);
            case CHANGE_STATUS -> booking.updateStatus(id, request.status());
            case ADD_TAG -> booking.updateTag(id, request.tag(), true);
            case REMOVE_TAG -> booking.updateTag(id, request.tag(), false);
        }
    }
}
