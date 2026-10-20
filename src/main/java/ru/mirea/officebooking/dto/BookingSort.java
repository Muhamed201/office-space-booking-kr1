package ru.mirea.officebooking.dto;

public enum BookingSort {
    START_TIME("Начало"),
    EMPLOYEE_NAME("Сотрудник"),
    WORKSPACE_CODE("Место"),
    DURATION("Длительность"),
    STATUS("Статус");

    private final String title;

    BookingSort(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
