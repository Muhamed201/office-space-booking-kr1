package ru.mirea.officebooking.exception;

public class AccessDeniedException extends BusinessRuleException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
