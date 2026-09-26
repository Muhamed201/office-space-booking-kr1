package ru.mirea.officebooking.exception;

public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException forId(String entityName, long id) {
        String message = String.format("Запись «%s» с id=%d не найдена", entityName, id);
        return new EntityNotFoundException(message);
    }
}
