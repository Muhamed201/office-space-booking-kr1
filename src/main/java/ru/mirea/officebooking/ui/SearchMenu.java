package ru.mirea.officebooking.ui;

import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.service.search.BookingSearchService;

public class SearchMenu {

    private final BookingSearchService searchService;
    private final BookingPrinter printer;
    private final ConsoleReader reader;

    public SearchMenu(BookingSearchService searchService, BookingPrinter printer, ConsoleReader reader) {
        this.searchService = searchService;
        this.printer = printer;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Поиск броней ---
                    1. По сотруднику (часть ФИО или email)
                    2. По коду места
                    3. По дате
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> printer.printSorted(searchService.searchByEmployee(reader.readLine("Часть ФИО или email: ")));
                    case 2 -> printer.printSorted(searchService.searchByWorkspaceCode(reader.readLine("Часть кода места: ")));
                    case 3 -> printer.printSorted(searchService.searchByDate(reader.readDate("Дата")));
                    case 0 -> inMenu = false;
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
