package ru.mirea.officebooking.service;

import java.util.List;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.TableDump;
import ru.mirea.officebooking.repository.TableDumpRepository;

public class DbTablesService {

    private final TableDumpRepository tableDumpRepository;

    public DbTablesService(TableDumpRepository tableDumpRepository) {
        this.tableDumpRepository = tableDumpRepository;
    }

    public List<String> tableNames() {
        return tableDumpRepository.tableNames();
    }

    public TableDump dump(String tableName) {
        if (tableName == null || !tableDumpRepository.tableNames().contains(tableName)) {
            throw new ValidationException("Неизвестная таблица: " + tableName);
        }
        return tableDumpRepository.dump(tableName);
    }
}
