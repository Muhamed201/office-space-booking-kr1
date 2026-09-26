package ru.mirea.officebooking.ui;

import java.util.NoSuchElementException;
import ru.mirea.officebooking.exception.AppException;

public class ConsoleApp {

    private final ConsoleReader reader;
    private final EmployeeMenu employeeMenu;
    private final WorkspaceMenu workspaceMenu;
    private final BookingMenu bookingMenu;
    private final SearchMenu searchMenu;
    private final FilterMenu filterMenu;
    private final StatisticsMenu statisticsMenu;
    private final ExportMenu exportMenu;
    private final DbTablesMenu dbTablesMenu;

    public ConsoleApp(ConsoleReader reader, EmployeeMenu employeeMenu, WorkspaceMenu workspaceMenu,
                      BookingMenu bookingMenu, SearchMenu searchMenu, FilterMenu filterMenu,
                      StatisticsMenu statisticsMenu, ExportMenu exportMenu, DbTablesMenu dbTablesMenu) {
        this.reader = reader;
        this.employeeMenu = employeeMenu;
        this.workspaceMenu = workspaceMenu;
        this.bookingMenu = bookingMenu;
        this.searchMenu = searchMenu;
        this.filterMenu = filterMenu;
        this.statisticsMenu = statisticsMenu;
        this.exportMenu = exportMenu;
        this.dbTablesMenu = dbTablesMenu;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("""

                    ======================================================
                            СИСТЕМА УПРАВЛЕНИЯ ОФИСНЫМИ МЕСТАМИ
                    ======================================================
                    1. Сотрудники
                    2. Рабочие места
                    3. Бронирования
                    4. Поиск
                    5. Фильтрация
                    6. Статистика
                    7. Экспорт данных
                    8. Показать таблицы базы данных
                    0. Выход""");
            try {
                int choice = reader.readInt("Выберите действие: ");
                switch (choice) {
                    case 1 -> employeeMenu.run();
                    case 2 -> workspaceMenu.run();
                    case 3 -> bookingMenu.run();
                    case 4 -> searchMenu.run();
                    case 5 -> filterMenu.run();
                    case 6 -> statisticsMenu.run();
                    case 7 -> exportMenu.run();
                    case 8 -> dbTablesMenu.run();
                    case 0 -> {
                        running = false;
                        System.out.println("До свидания!");
                    }
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (NoSuchElementException e) {
                System.out.println("\nВвод закрыт, завершаю работу.");
                running = false;
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
    }
}
