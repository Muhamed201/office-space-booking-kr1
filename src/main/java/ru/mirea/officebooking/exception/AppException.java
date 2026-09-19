package ru.mirea.officebooking.exception;

public abstract class AppException extends RuntimeException {
  protected AppException(String message) {
    super(message);
  }
  protected AppException(String message, Throwable cause){ super(message, cause); }
}

