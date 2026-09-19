package ru.mirea.officebooking.exception;

public class EntityNotFoundException extends AppException{
  public EntityNotFoundException(String message){
    super(message);
  }
  public static EntityNotFoundException forId(String entityClass, long id) {
    String message = String.format("Entity %s with id %d not found", entityClass, id);
    return new EntityNotFoundException(message);

  }

}
