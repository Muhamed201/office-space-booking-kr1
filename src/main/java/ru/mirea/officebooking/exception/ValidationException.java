package ru.mirea.officebooking.exception;

public class ValidationException extends BusinessRuleException{
  public ValidationException(String message){
    super(message);
  }
}
