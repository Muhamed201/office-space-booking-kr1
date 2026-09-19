package ru.mirea.officebooking.exception;

public class InvalidStatusTransitionException extends BusinessRuleException{
  public InvalidStatusTransitionException(String message){
    super(message);
  }
}
