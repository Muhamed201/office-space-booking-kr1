package ru.mirea.officebooking.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;

class BookingRowExportersTest {

    @TempDir
    Path tempDir;

    private static BookingRow row(long id, String purpose) {
        return new BookingRow(id, 1, "Иванов Иван Иванович", "ivanov@example.org", 2, "A-18",
                WorkspaceType.OPEN_DESK, 2, LocalDateTime.of(2026, 10, 20, 9, 0),
                LocalDateTime.of(2026, 10, 20, 11, 0), BookingStatus.ACTIVE, null, purpose);
    }

    @Test
    void csvHasHeaderAndOneLinePerRow() throws IOException {
        Path file = tempDir.resolve("bookings.csv");

        new CsvBookingExporter().export(List.of(row(1, "Планёрка"), row(2, null)), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("Сотрудник"));
        assertTrue(lines.get(1).contains("Иванов Иван Иванович"));
    }

    @Test
    void csvEscapesSeparatorAndQuotesInPurpose() throws IOException {
        Path file = tempDir.resolve("bookings.csv");

        new CsvBookingExporter().export(List.of(row(1, "цель; \"важная\"")), file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"цель; \"\"важная\"\"\""));
    }

    @Test
    void xlsxHasHeaderAndOneRowPerBooking() throws IOException {
        Path file = tempDir.resolve("bookings.xlsx");

        new XlsxBookingExporter().export(List.of(row(1, "Планёрка"), row(2, null), row(3, null)), file);

        try (InputStream in = Files.newInputStream(file); XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(3, sheet.getLastRowNum());
            assertEquals("Сотрудник", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Иванов Иван Иванович", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(1.0, sheet.getRow(1).getCell(0).getNumericCellValue());
        }
    }

    @Test
    void emptyListProducesOnlyHeader() throws IOException {
        Path file = tempDir.resolve("empty.csv");

        new CsvBookingExporter().export(List.of(), file);

        assertEquals(1, Files.readAllLines(file, StandardCharsets.UTF_8).size());
    }
}
