package ru.mirea.officebooking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.officebooking.exception.ValidationException;

class ExportServiceRowsTest {

    @TempDir
    Path tempDir;

    private final ExportService service = new ExportService(null, null, null, List.of());

    @Test
    void unsupportedExtensionIsRejected() {
        assertThrows(ValidationException.class, () -> service.exportRows(List.of(), tempDir.resolve("data.txt")));
    }

    @Test
    void nullTargetIsRejected() {
        assertThrows(ValidationException.class, () -> service.exportRows(List.of(), null));
    }

    @Test
    void nullRowsAreRejected() {
        assertThrows(ValidationException.class, () -> service.exportRows(null, tempDir.resolve("data.csv")));
    }

    @Test
    void extensionIsMatchedCaseInsensitively() throws Exception {
        Path target = tempDir.resolve("DATA.CSV");

        service.exportRows(List.of(), target);

        assertTrue(Files.exists(target));
    }

    @Test
    void xlsxFileIsCreated() throws Exception {
        Path target = tempDir.resolve("data.xlsx");

        service.exportRows(List.of(), target);

        assertTrue(Files.size(target) > 0);
    }
}
