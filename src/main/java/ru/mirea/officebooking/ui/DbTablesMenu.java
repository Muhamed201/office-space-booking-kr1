package ru.mirea.officebooking.ui;

import ru.mirea.officebooking.util.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Scanner;

public class DbTablesMenu {
    private final DatabaseManager dbManager;
    private final Scanner scanner;

    public DbTablesMenu(DatabaseManager dbManager, Scanner scanner) {
        this.dbManager = dbManager;
        this.scanner = scanner;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== Просмотр таблиц БД (read-only) ===");
            System.out.println("1. employee");
            System.out.println("2. workspace");
            System.out.println("3. booking");
            System.out.println("0. Назад");
            System.out.print("Ваш выбор: ");

            String choice = scanner.nextLine();
            String tableName = "";
            switch (choice) {
                case "1" -> tableName = "employee";
                case "2" -> tableName = "workspace";
                case "3" -> tableName = "booking";
                case "0" -> { return; }
                default -> {
                    System.out.println("Нет такого пункта.");
                    continue;
                }
            }
            printTable(tableName);
        }
    }

    private void printTable(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        try {
            Connection conn = dbManager.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Вывод заголовков
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-25s", metaData.getColumnName(i));
                }
                System.out.println();
                System.out.println("-".repeat(columnCount * 25));

                // Вывод строк
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String value = rs.getString(i);
                        System.out.printf("%-25s", value == null ? "NULL" : value);
                    }
                    System.out.println();
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка при чтении таблицы " + tableName + ": " + e.getMessage());
        }
    }
}