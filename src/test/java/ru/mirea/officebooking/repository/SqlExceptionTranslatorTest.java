package ru.mirea.officebooking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.exception.ValidationException;

class SqlExceptionTranslatorTest {

    private static AppException translate(String state, String message) {
        return SqlExceptionTranslator.translate("Действие", new SQLException(message, state));
    }

    @Test
    void duplicateEmailBecomesValidationError() {
        AppException result = translate("23505", "duplicate key value violates unique constraint \"employee_email_key\"");
        assertInstanceOf(ValidationException.class, result);
        assertTrue(result.getMessage().contains("email"));
    }

    @Test
    void duplicateWorkspaceCodeBecomesValidationError() {
        AppException result = translate("23505", "duplicate key value violates unique constraint \"workspace_code_key\"");
        assertInstanceOf(ValidationException.class, result);
        assertTrue(result.getMessage().contains("кодом"));
    }

    @Test
    void foreignKeyViolationsAreDistinguishedByDirection() {
        AppException onDelete = translate("23503", "update or delete on table \"employee\" violates foreign key constraint");
        AppException onInsert = translate("23503", "insert or update on table \"booking\" violates foreign key constraint");
        assertInstanceOf(ValidationException.class, onDelete);
        assertInstanceOf(ValidationException.class, onInsert);
        assertTrue(onDelete.getMessage().contains("ссылаются"));
        assertTrue(onInsert.getMessage().contains("несуществующая"));
    }

    @Test
    void notNullAndCheckViolationsBecomeValidationErrors() {
        assertInstanceOf(ValidationException.class, translate("23502", "null value in column"));
        assertInstanceOf(ValidationException.class, translate("23514", "violates check constraint"));
    }

    @Test
    void connectionProblemsBecomeDatabaseException() {
        AppException result = translate("08006", "connection failure");
        assertInstanceOf(DatabaseException.class, result);
        assertTrue(result.getMessage().contains("соединение"));
    }

    @Test
    void unknownErrorsKeepOriginalCause() {
        SQLException original = new SQLException("relation does not exist", "42P01");
        AppException result = SqlExceptionTranslator.translate("Не удалось получить список", original);
        assertInstanceOf(DatabaseException.class, result);
        assertEquals("Не удалось получить список", result.getMessage());
        assertSame(original, result.getCause());
    }
}
