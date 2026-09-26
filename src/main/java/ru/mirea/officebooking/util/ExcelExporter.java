package ru.mirea.officebooking.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.officebooking.exception.ExportException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;

public class ExcelExporter implements Exporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final DateTimeFormatter CELL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path exportDir;

    public ExcelExporter() {
        this(Path.of("exports"));
    }

    public ExcelExporter(Path exportDir) {
        this.exportDir = exportDir;
    }

    @Override
    public String formatName() {
        return "Excel (.xlsx)";
    }

    @Override
    public String export(List<Employee> employees, List<Workspace> workspaces, List<Booking> bookings) {
        Path file = exportDir.resolve("office-booking-%s.xlsx".formatted(LocalDateTime.now().format(FILE_STAMP)));
        try {
            Files.createDirectories(exportDir);
            try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
                CellStyle header = headerStyle(workbook);
                writeEmployees(workbook.createSheet("Employees"), header, employees);
                writeWorkspaces(workbook.createSheet("Workspaces"), header, workspaces);
                writeBookings(workbook.createSheet("Bookings"), header, bookings);
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось записать Excel-файл " + file.toAbsolutePath()
                    + " (возможно, он открыт в другой программе или нет прав на запись)", e);
        }
        return file.toAbsolutePath().toString();
    }

    private void writeEmployees(Sheet sheet, CellStyle header, List<Employee> employees) {
        String[] columns = {"id", "full_name", "email", "department", "position", "hire_date", "blocked", "role"};
        writeRow(sheet, 0, header, (Object[]) columns);
        int rowIndex = 1;
        for (Employee e : employees) {
            writeRow(sheet, rowIndex++, null, e.getId(), e.getFullName(), e.getEmail(), e.getDepartment(),
                    e.getPosition(), e.getHireDate().toString(), e.isBlocked(), e.getRole().name());
        }
        finish(sheet, columns.length);
    }

    private void writeWorkspaces(Sheet sheet, CellStyle header, List<Workspace> workspaces) {
        String[] columns = {"id", "code", "floor", "type", "status", "has_monitor", "capacity"};
        writeRow(sheet, 0, header, (Object[]) columns);
        int rowIndex = 1;
        for (Workspace w : workspaces) {
            writeRow(sheet, rowIndex++, null, w.getId(), w.getCode(), w.getFloor(), w.type().name(),
                    w.getStatus().name(), w.isHasMonitor(), w.getCapacity());
        }
        finish(sheet, columns.length);
    }

    private void writeBookings(Sheet sheet, CellStyle header, List<Booking> bookings) {
        String[] columns = {"id", "employee_id", "workspace_id", "start_time", "end_time", "status", "attendees", "purpose"};
        writeRow(sheet, 0, header, (Object[]) columns);
        int rowIndex = 1;
        for (Booking b : bookings) {
            writeRow(sheet, rowIndex++, null, b.getId(), b.getEmployeeId(), b.getWorkspaceId(),
                    b.getStartTime().format(CELL_DATE_TIME), b.getEndTime().format(CELL_DATE_TIME),
                    b.getStatus().name(), b.getAttendees(), b.getPurpose());
        }
        finish(sheet, columns.length);
    }

    private void writeRow(Sheet sheet, int rowIndex, CellStyle style, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else if (value instanceof Boolean flag) {
                cell.setCellValue(flag);
            } else if (value != null) {
                cell.setCellValue(value.toString());
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private void finish(Sheet sheet, int columnCount) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }
}
