package ru.mirea.officebooking.util;

import java.nio.file.Path;
import java.util.List;
import ru.mirea.officebooking.dto.BookingRow;

public interface BookingRowExporter {

    String extension();

    void export(List<BookingRow> rows, Path target);
}
