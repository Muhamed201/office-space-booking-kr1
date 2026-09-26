package ru.mirea.officebooking;

import java.util.List;
import java.util.Scanner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.TableDumpRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;
import ru.mirea.officebooking.service.BookingService;
import ru.mirea.officebooking.service.DbTablesService;
import ru.mirea.officebooking.service.EmployeeService;
import ru.mirea.officebooking.service.ExportService;
import ru.mirea.officebooking.service.StatisticsService;
import ru.mirea.officebooking.service.WorkspaceService;
import ru.mirea.officebooking.service.search.BookingSearchService;
import ru.mirea.officebooking.ui.BookingMenu;
import ru.mirea.officebooking.ui.BookingPrinter;
import ru.mirea.officebooking.ui.ConsoleApp;
import ru.mirea.officebooking.ui.ConsoleReader;
import ru.mirea.officebooking.ui.DbTablesMenu;
import ru.mirea.officebooking.ui.EmployeeMenu;
import ru.mirea.officebooking.ui.ExportMenu;
import ru.mirea.officebooking.ui.FilterMenu;
import ru.mirea.officebooking.ui.SearchMenu;
import ru.mirea.officebooking.ui.StatisticsMenu;
import ru.mirea.officebooking.ui.WorkspaceMenu;
import ru.mirea.officebooking.util.CsvExporter;
import ru.mirea.officebooking.util.DatabaseManager;
import ru.mirea.officebooking.util.ExcelExporter;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            start();
        } catch (AppException e) {
            System.err.println("Не удалось запустить приложение: " + e.getMessage());
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                System.err.println("Причина: " + e.getCause().getMessage().lines().findFirst().orElse(""));
            }
            System.exit(1);
        }
    }

    private static void start() {
        DatabaseManager databaseManager = new DatabaseManager();
        runMigrations(databaseManager);

        EmployeeRepository employeeRepository = new EmployeeRepository(databaseManager);
        WorkspaceRepository workspaceRepository = new WorkspaceRepository(databaseManager);
        BookingRepository bookingRepository = new BookingRepository(databaseManager);
        TableDumpRepository tableDumpRepository = new TableDumpRepository(databaseManager);

        EmployeeService employeeService = new EmployeeService(employeeRepository, bookingRepository);
        WorkspaceService workspaceService = new WorkspaceService(workspaceRepository, bookingRepository);
        BookingService bookingService = new BookingService(bookingRepository, employeeRepository, workspaceRepository);
        BookingSearchService searchService = new BookingSearchService(bookingRepository);
        StatisticsService statisticsService =
                new StatisticsService(employeeRepository, workspaceRepository, bookingRepository);
        ExportService exportService = new ExportService(employeeRepository, workspaceRepository, bookingRepository,
                List.of(new ExcelExporter(), new CsvExporter()));
        DbTablesService dbTablesService = new DbTablesService(tableDumpRepository);

        ConsoleReader reader = new ConsoleReader(new Scanner(System.in));
        BookingPrinter printer = new BookingPrinter(employeeService, workspaceService, reader);

        ConsoleApp app = new ConsoleApp(
                reader,
                new EmployeeMenu(employeeService, reader),
                new WorkspaceMenu(workspaceService, reader),
                new BookingMenu(bookingService, printer, reader),
                new SearchMenu(searchService, printer, reader),
                new FilterMenu(searchService, printer, reader),
                new StatisticsMenu(statisticsService, reader),
                new ExportMenu(exportService, reader),
                new DbTablesMenu(dbTablesService, reader));
        app.run();
    }

    private static void runMigrations(DatabaseManager databaseManager) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(databaseManager.getUrl(), databaseManager.getUser(), databaseManager.getPassword())
                    .load();
            flyway.migrate();
        } catch (FlywayException e) {
            throw new DatabaseException("Не удалось применить миграции базы данных", e);
        }
    }
}
