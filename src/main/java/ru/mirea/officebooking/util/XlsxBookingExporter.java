package ru.mirea.officebooking.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.exception.ExportException;

public class XlsxBookingExporter implements BookingRowExporter {

    private static final String SHEET_NAME = "Брони";

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public void export(List<BookingRow> rows, Path target) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeRow(sheet, 0, headerStyle(workbook), List.copyOf(BookingRowColumns.HEADERS));
            int rowIndex = 1;
            for (BookingRow row : rows) {
                writeRow(sheet, rowIndex++, null, BookingRowColumns.values(row));
            }
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < BookingRowColumns.HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        } catch (IOException e) {
            throw new ExportException("Не удалось записать файл " + target.toAbsolutePath()
                    + " (возможно, он открыт в другой программе или нет прав на запись)", e);
        }
    }

    private static void writeRow(Sheet sheet, int rowIndex, CellStyle style, List<?> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            Object value = values.get(i);
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(String.valueOf(value));
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }
}
