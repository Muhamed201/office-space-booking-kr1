package ru.mirea.officebooking.dto;

import java.time.LocalDate;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public record BookingFilter(
        String text,
        BookingStatus status,
        WorkspaceType workspaceType,
        Integer floor,
        LocalDate dateFrom,
        LocalDate dateTo) {

    public static final int MAX_TEXT_LENGTH = 100;

    public BookingFilter {
        text = text == null || text.isBlank() ? null : text.trim();
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Строка поиска не должна быть длиннее %d символов".formatted(MAX_TEXT_LENGTH));
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException("Дата «с» не может быть позже даты «по»");
        }
    }

    public static BookingFilter empty() {
        return new BookingFilter(null, null, null, null, null, null);
    }
}
