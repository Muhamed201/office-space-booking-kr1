package ru.mirea.officebooking.model;

import ru.mirea.officebooking.exception.ValidationException;

public final class Validation {

    private Validation() {
    }

    public static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new ValidationException("Поле «" + field + "» обязательно для заполнения");
        }
        return value;
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Поле «" + field + "» не может быть пустым");
        }
        String trimmed = value.trim();
        checkText(trimmed, field, maxLength);
        return trimmed;
    }

    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        checkText(trimmed, field, maxLength);
        return trimmed;
    }

    public static long requirePositiveId(Long id, String field) {
        if (id == null) {
            throw new ValidationException("Поле «" + field + "» обязательно для заполнения");
        }
        if (id <= 0) {
            throw new ValidationException("Поле «" + field + "» должно быть положительным числом, получено: " + id);
        }
        return id;
    }

    public static Long optionalPositiveId(Long id, String field) {
        if (id == null) {
            return null;
        }
        return requirePositiveId(id, field);
    }

    public static int requireInRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new ValidationException(
                    "Поле «%s» должно быть в диапазоне от %d до %d, получено: %d".formatted(field, min, max, value));
        }
        return value;
    }

    private static void checkText(String text, String field, int maxLength) {
        if (text.length() > maxLength) {
            throw new ValidationException(
                    "Поле «%s» не должно быть длиннее %d символов (введено %d)".formatted(field, maxLength, text.length()));
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                throw new ValidationException("Поле «" + field + "» содержит недопустимые управляющие символы");
            }
        }
    }
}
