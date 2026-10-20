package ru.mirea.officebooking.fx.util;

import ru.mirea.officebooking.exception.AppException;

public final class ErrorMessages {

    public static final String UNEXPECTED = "Произошла внутренняя ошибка. Подробности записаны в журнал.";

    private static final System.Logger LOGGER = System.getLogger(ErrorMessages.class.getName());

    private ErrorMessages() {
    }

    public static String of(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof AppException) {
                return current.getMessage();
            }
        }
        LOGGER.log(System.Logger.Level.ERROR, "Непредвиденная ошибка", error);
        return UNEXPECTED;
    }
}
