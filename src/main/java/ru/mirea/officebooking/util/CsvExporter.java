package ru.mirea.officebooking.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.mirea.officebooking.exception.ExportException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;

public class CsvExporter implements Exporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final DateTimeFormatter CELL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SEPARATOR = ";";
    private static final String BOM = "﻿";

    private final Path exportDir;

    public CsvExporter() {
        this(Path.of("exports"));
    }

    public CsvExporter(Path exportDir) {
        this.exportDir = exportDir;
    }

    @Override
    public String formatName() {
        return "CSV (три файла)";
    }

    @Override
    public String export(List<Employee> employees, List<Workspace> workspaces, List<Booking> bookings) {
        Path targetDir = exportDir.resolve("office-booking-csv-" + LocalDateTime.now().format(FILE_STAMP));
        try {
            Files.createDirectories(targetDir);
            writeFile(targetDir.resolve("employees.csv"),
                    List.of("id", "full_name", "email", "department", "position", "hire_date", "blocked", "role"),
                    employees.stream().map(e -> List.<Object>of(e.getId(), e.getFullName(), e.getEmail(),
                            e.getDepartment(), nullToEmpty(e.getPosition()), e.getHireDate(), e.isBlocked(),
                            e.getRole())).toList());
            writeFile(targetDir.resolve("workspaces.csv"),
                    List.of("id", "code", "floor", "type", "status", "has_monitor", "capacity"),
                    workspaces.stream().map(w -> List.<Object>of(w.getId(), w.getCode(), w.getFloor(), w.type(),
                            w.getStatus(), w.isHasMonitor(), w.getCapacity())).toList());
            writeFile(targetDir.resolve("bookings.csv"),
                    List.of("id", "employee_id", "workspace_id", "start_time", "end_time", "status", "attendees",
                            "purpose"),
                    bookings.stream().map(b -> List.<Object>of(b.getId(), b.getEmployeeId(), b.getWorkspaceId(),
                            b.getStartTime().format(CELL_DATE_TIME), b.getEndTime().format(CELL_DATE_TIME),
                            b.getStatus(), b.getAttendees() == null ? "" : b.getAttendees(),
                            nullToEmpty(b.getPurpose()))).toList());
        } catch (IOException e) {
            throw new ExportException("Не удалось записать CSV-файлы в " + targetDir.toAbsolutePath(), e);
        }
        return targetDir.toAbsolutePath().toString();
    }

    private void writeFile(Path path, List<String> header, List<List<Object>> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writer.write(String.join(SEPARATOR, header));
            writer.newLine();
            for (List<Object> row : rows) {
                writer.write(row.stream().map(v -> escape(String.valueOf(v))).collect(Collectors.joining(SEPARATOR)));
                writer.newLine();
            }
        }
    }

    private static String escape(String value) {
        String safe = value;
        if (Stream.of("=", "+", "-", "@").anyMatch(safe::startsWith) && !isNumber(safe)) {
            safe = "'" + safe;
        }
        if (safe.contains(SEPARATOR) || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
