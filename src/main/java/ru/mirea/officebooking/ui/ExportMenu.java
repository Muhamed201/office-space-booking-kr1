package ru.mirea.officebooking.ui;

import ru.mirea.officebooking.util.ExcelExporter; // Класс Мухамеда

public class ExportMenu {
    private final ExcelExporter excelExporter;
    private final ConsoleReader reader;

    public ExportMenu(ExcelExporter excelExporter, ConsoleReader reader) {
        this.excelExporter = excelExporter;
        this.reader = reader;
    }

    public void show() {
        System.out.println("\n=== Меню экспорта ===");
        System.out.println("1. Экспортировать рабочие места в Excel");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        int choice = reader.readInt();
        if (choice == 1) {
            try {
                String filename = "exports/workspaces_export.xlsx";
                excelExporter.exportWorkspaces(filename); // Вызов метода Мухамеда
                System.out.println("Экспорт успешно завершен! Файл: " + filename);
            } catch (Exception e) {
                System.out.println("Ошибка при экспорте: " + e.getMessage());
            }
        }
    }
}