package ru.mirea.officebooking.ui;

import java.time.LocalDateTime;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.service.search.BookingSearchService;

public class FilterMenu {

    private final BookingSearchService searchService;
    private final BookingPrinter printer;
    private final ConsoleReader reader;

    public FilterMenu(BookingSearchService searchService, BookingPrinter printer, ConsoleReader reader) {
        this.searchService = searchService;
        this.printer = printer;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Фильтрация броней ---
                    1. По статусу
                    2. По типу места
                    3. По этажу
                    4. По диапазону дат
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> printer.printSorted(
                            searchService.filterByStatus(reader.readEnum(BookingStatus.class, "Статус:")));
                    case 2 -> printer.printSorted(
                            searchService.filterByWorkspaceType(reader.readEnum(WorkspaceType.class, "Тип места:")));
                    case 3 -> printer.printSorted(searchService.filterByFloor(
                            reader.readIntInRange("Этаж: ", Workspace.MIN_FLOOR, Workspace.MAX_FLOOR)));
                    case 4 -> filterByRange();
                    case 0 -> inMenu = false;
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void filterByRange() {
        LocalDateTime from = reader.readDateTime("С");
        LocalDateTime to = reader.readDateTime("По");
        printer.printSorted(searchService.filterByDateRange(from, to));
    }
}
