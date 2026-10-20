package ru.mirea.officebooking.fx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.ValidationException;

class ErrorMessagesTest {

    @Test
    void appExceptionShowsItsOwnMessage() {
        assertEquals("Неверная дата", ErrorMessages.of(new ValidationException("Неверная дата")));
    }

    @Test
    void appExceptionWrappedInOtherExceptionIsUnwrapped() {
        IOException wrapper = new IOException("обёртка", new ValidationException("Причина"));

        assertEquals("Причина", ErrorMessages.of(wrapper));
    }

    @Test
    void unexpectedExceptionDoesNotLeakDetails() {
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(new IllegalStateException("секретные детали")));
    }
}
