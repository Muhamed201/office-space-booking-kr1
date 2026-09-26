package ru.mirea.officebooking.service;

import java.util.List;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;
import ru.mirea.officebooking.util.Exporter;

public class ExportService {

    private final EmployeeRepository employeeRepository;
    private final WorkspaceRepository workspaceRepository;
    private final BookingRepository bookingRepository;
    private final List<Exporter> exporters;

    public ExportService(EmployeeRepository employeeRepository, WorkspaceRepository workspaceRepository,
                         BookingRepository bookingRepository, List<Exporter> exporters) {
        this.employeeRepository = employeeRepository;
        this.workspaceRepository = workspaceRepository;
        this.bookingRepository = bookingRepository;
        this.exporters = List.copyOf(exporters);
    }

    public List<Exporter> availableExporters() {
        return exporters;
    }

    public String export(Exporter exporter) {
        Validation.requireNotNull(exporter, "Формат экспорта");
        return exporter.export(employeeRepository.findAll(), workspaceRepository.findAll(), bookingRepository.findAll());
    }
}
