package ru.mirea.officebooking.ui;

import java.util.List;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.TableDump;
import ru.mirea.officebooking.service.DbTablesService;

public class DbTablesMenu {

    private static final int MAX_COLUMN_WIDTH = 40;

    private final DbTablesService dbTablesService;
    private final ConsoleReader reader;

    public DbTablesMenu(DbTablesService dbTablesService, ConsoleReader reader) {
        this.dbTablesService = dbTablesService;
        this.reader = reader;
    }

    public void run() {
        List<String> tables = dbTablesService.tableNames();
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Таблицы базы данных ---");
            for (int i = 0; i < tables.size(); i++) {
                String note = tables.get(i).equals("flyway_schema_history") ? " (служебная: история миграций)" : "";
                System.out.printf("%d. %s%s%n", i + 1, tables.get(i), note);
            }
            System.out.println("0. Назад");
            int choice = reader.readInt("Выбор: ");
            try {
                if (choice == 0) {
                    inMenu = false;
                } else if (choice >= 1 && choice <= tables.size()) {
                    print(dbTablesService.dump(tables.get(choice - 1)));
                } else {
                    System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void print(TableDump dump) {
        int[] widths = new int[dump.columns().size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = dump.columns().get(i).length();
        }
        for (List<String> row : dump.rows()) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], Math.min(row.get(i).length(), MAX_COLUMN_WIDTH));
            }
        }
        System.out.println("Таблица " + dump.tableName() + ", строк: " + dump.rows().size());
        printRow(dump.columns(), widths);
        for (List<String> row : dump.rows()) {
            printRow(row, widths);
        }
    }

    private void printRow(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String cell = cells.get(i);
            if (cell.length() > MAX_COLUMN_WIDTH) {
                cell = cell.substring(0, MAX_COLUMN_WIDTH - 1) + "…";
            }
            line.append(String.format("%-" + widths[i] + "s  ", cell));
        }
        System.out.println(line.toString().stripTrailing());
    }
}
