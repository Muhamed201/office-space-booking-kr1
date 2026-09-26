package ru.mirea.officebooking.repository;

import java.sql.SQLException;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.exception.ValidationException;

final class SqlExceptionTranslator {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String NOT_NULL_VIOLATION = "23502";
    private static final String CHECK_VIOLATION = "23514";
    private static final String CONNECTION_CLASS = "08";

    private SqlExceptionTranslator() {
    }

    static AppException translate(String action, SQLException e) {
        String state = e.getSQLState();
        String details = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (UNIQUE_VIOLATION.equals(state)) {
            if (details.contains("email")) {
                return new ValidationException(action + ": сотрудник с таким email уже существует");
            }
            if (details.contains("code")) {
                return new ValidationException(action + ": рабочее место с таким кодом уже существует");
            }
            return new ValidationException(action + ": запись с такими уникальными данными уже существует");
        }
        if (FOREIGN_KEY_VIOLATION.equals(state)) {
            if (details.contains("update or delete")) {
                return new ValidationException(action + ": на эту запись ссылаются другие данные (например, брони)");
            }
            return new ValidationException(action + ": указана несуществующая связанная запись");
        }
        if (NOT_NULL_VIOLATION.equals(state)) {
            return new ValidationException(action + ": не заполнено обязательное поле");
        }
        if (CHECK_VIOLATION.equals(state)) {
            return new ValidationException(action + ": данные нарушают ограничение базы данных");
        }
        if (state != null && state.startsWith(CONNECTION_CLASS)) {
            return new DatabaseException(action + ": потеряно соединение с базой данных", e);
        }
        return new DatabaseException(action, e);
    }
}
