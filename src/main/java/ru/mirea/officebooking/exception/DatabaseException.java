package ru.mirea.officebooking.exception;

public class DatabaseException extends AppException{
  public DatabaseException(String message, Throwable cause){
    super(message, cause);
  }
}
