package ru.mirea.officebooking.util;

import java.time.format.DateTimeFormatter;
import java.util.List;
import ru.mirea.officebooking.dto.BookingRow;

final class BookingRowColumns {

    static final List<String> HEADERS = List.of(
            "№", "Сотрудник", "Email", "Место", "Тип", "Этаж", "Начало", "Конец", "Статус", "Участников", "Цель");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BookingRowColumns() {
    }

    static List<Object> values(BookingRow row) {
        return List.of(
                row.id(),
                row.employeeName(),
                row.employeeEmail(),
                row.workspaceCode(),
                row.workspaceType().name(),
                row.floor(),
                row.startTime().format(DATE_TIME),
                row.endTime().format(DATE_TIME),
                row.status().name(),
                row.attendees() == null ? "" : row.attendees(),
                row.purpose() == null ? "" : row.purpose());
    }
}
