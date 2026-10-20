package ru.mirea.officebooking.fx.util;

public interface FormController {

    Runnable prepareSave();

    void showError(String message);
}
