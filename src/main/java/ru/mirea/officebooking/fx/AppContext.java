package ru.mirea.officebooking.fx;

import java.util.List;
import ru.mirea.officebooking.repository.BookingQueryRepository;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;
import ru.mirea.officebooking.service.BookingQueryService;
import ru.mirea.officebooking.service.BookingService;
import ru.mirea.officebooking.service.EmployeeService;
import ru.mirea.officebooking.service.ExportService;
import ru.mirea.officebooking.service.StatisticsService;
import ru.mirea.officebooking.service.WorkspaceService;
import ru.mirea.officebooking.util.CsvExporter;
import ru.mirea.officebooking.util.DatabaseManager;
import ru.mirea.officebooking.util.DatabaseMigrator;
import ru.mirea.officebooking.util.ExcelExporter;

public final class AppContext {

    private final BookingService bookings;
    private final BookingQueryService bookingQueries;
    private final EmployeeService employees;
    private final WorkspaceService workspaces;
    private final StatisticsService statistics;
    private final ExportService export;
    private final Session session = new Session();

    public AppContext() {
        DatabaseManager databaseManager = new DatabaseManager();
        DatabaseMigrator.migrate(databaseManager);

        EmployeeRepository employeeRepository = new EmployeeRepository(databaseManager);
        WorkspaceRepository workspaceRepository = new WorkspaceRepository(databaseManager);
        BookingRepository bookingRepository = new BookingRepository(databaseManager);
        BookingQueryRepository bookingQueryRepository = new BookingQueryRepository(databaseManager);

        this.employees = new EmployeeService(employeeRepository, bookingRepository);
        this.workspaces = new WorkspaceService(workspaceRepository, bookingRepository, employeeRepository);
        this.bookings = new BookingService(bookingRepository, employeeRepository, workspaceRepository);
        this.bookingQueries = new BookingQueryService(bookingQueryRepository);
        this.statistics = new StatisticsService(employeeRepository, workspaceRepository, bookingRepository,
                bookingQueryRepository);
        this.export = new ExportService(employeeRepository, workspaceRepository, bookingRepository,
                List.of(new ExcelExporter(), new CsvExporter()));
    }

    public BookingService bookings() {
        return bookings;
    }

    public BookingQueryService bookingQueries() {
        return bookingQueries;
    }

    public EmployeeService employees() {
        return employees;
    }

    public WorkspaceService workspaces() {
        return workspaces;
    }

    public StatisticsService statistics() {
        return statistics;
    }

    public ExportService export() {
        return export;
    }

    public Session session() {
        return session;
    }
}
