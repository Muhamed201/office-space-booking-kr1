package ru.mirea.officebooking.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.exception.ExportException;

public class CsvBookingExporter implements BookingRowExporter {

    private static final String SEPARATOR = ";";
    private static final String BOM = "﻿";

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public void export(List<BookingRow> rows, Path target) {
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writer.write(String.join(SEPARATOR, BookingRowColumns.HEADERS));
            writer.newLine();
            for (BookingRow row : rows) {
                writer.write(BookingRowColumns.values(row).stream()
                        .map(value -> CsvExporter.escape(String.valueOf(value)))
                        .collect(Collectors.joining(SEPARATOR)));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось записать файл " + target.toAbsolutePath()
                    + " (возможно, он открыт в другой программе или нет прав на запись)", e);
        }
    }
}
