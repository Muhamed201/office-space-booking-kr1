package ru.mirea.officebooking.exception;

public class BookingConflictException extends BusinessRuleException {
  public BookingConflictException(String message) {
    super(message);
  }
}
