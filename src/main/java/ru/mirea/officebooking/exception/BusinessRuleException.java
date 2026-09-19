package ru.mirea.officebooking.exception;

public abstract class BusinessRuleException extends AppException{
  protected BusinessRuleException(String message){
    super(message);
  }
  protected BusinessRuleException(String message, Throwable cause){
    super(message, cause);
  }
}
