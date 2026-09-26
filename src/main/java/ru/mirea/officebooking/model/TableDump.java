package ru.mirea.officebooking.model;

import java.util.List;

public record TableDump(String tableName, List<String> columns, List<List<String>> rows) {
}
