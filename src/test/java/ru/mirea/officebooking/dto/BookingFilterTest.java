package ru.mirea.officebooking.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.ValidationException;

class BookingFilterTest {

    @Test
    void emptyFilterHasNoCriteria() {
        BookingFilter filter = BookingFilter.empty();

        assertNull(filter.text());
        assertNull(filter.status());
        assertNull(filter.workspaceType());
        assertNull(filter.floor());
        assertNull(filter.dateFrom());
        assertNull(filter.dateTo());
    }

    @Test
    void blankTextBecomesNull() {
        assertNull(new BookingFilter("   ", null, null, null, null, null).text());
    }

    @Test
    void textIsTrimmed() {
        assertEquals("иванов", new BookingFilter("  иванов ", null, null, null, null, null).text());
    }

    @Test
    void tooLongTextIsRejected() {
        String longText = "а".repeat(BookingFilter.MAX_TEXT_LENGTH + 1);

        assertThrows(ValidationException.class, () -> new BookingFilter(longText, null, null, null, null, null));
    }

    @Test
    void dateFromAfterDateToIsRejected() {
        LocalDate from = LocalDate.of(2026, 10, 10);
        LocalDate to = LocalDate.of(2026, 10, 1);

        assertThrows(ValidationException.class, () -> new BookingFilter(null, null, null, null, from, to));
    }

    @Test
    void sameDayPeriodIsAllowed() {
        LocalDate day = LocalDate.of(2026, 10, 10);

        assertEquals(day, new BookingFilter(null, null, null, null, day, day).dateFrom());
    }
}
