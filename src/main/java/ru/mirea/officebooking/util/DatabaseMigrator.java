package ru.mirea.officebooking.util;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import ru.mirea.officebooking.exception.DatabaseException;

public final class DatabaseMigrator {

    private DatabaseMigrator() {
    }

    public static void migrate(DatabaseManager databaseManager) {
        try {
            Flyway.configure()
                    .dataSource(databaseManager.getUrl(), databaseManager.getUser(), databaseManager.getPassword())
                    .load()
                    .migrate();
        } catch (FlywayException e) {
            throw new DatabaseException("Не удалось применить миграции базы данных", e);
        }
    }
}
