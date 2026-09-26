package ru.mirea.officebooking.ui;

import java.util.List;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.service.ExportService;
import ru.mirea.officebooking.util.Exporter;

public class ExportMenu {

    private final ExportService exportService;
    private final ConsoleReader reader;

    public ExportMenu(ExportService exportService, ConsoleReader reader) {
        this.exportService = exportService;
        this.reader = reader;
    }

    public void run() {
        List<Exporter> exporters = exportService.availableExporters();
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Экспорт данных ---");
            for (int i = 0; i < exporters.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, exporters.get(i).formatName());
            }
            System.out.println("0. Назад");
            int choice = reader.readInt("Выбор: ");
            try {
                if (choice == 0) {
                    inMenu = false;
                } else if (choice >= 1 && choice <= exporters.size()) {
                    String path = exportService.export(exporters.get(choice - 1));
                    System.out.println("Экспортировано: " + path);
                } else {
                    System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
