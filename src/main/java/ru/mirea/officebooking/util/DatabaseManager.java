package ru.mirea.officebooking.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import ru.mirea.officebooking.exception.DatabaseException;

public class DatabaseManager {

    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        try (InputStream inputStream = DatabaseManager.class.getResourceAsStream("/application.properties")) {
            if (inputStream == null) {
                throw new DatabaseException("Не удалось инициализировать данные БД",
                        new FileNotFoundException("Resource /application.properties not found in classpath"));
            }
            props.load(inputStream);
            url = requireConfigured(resolve("DB_URL", "db.url", props), "DB_URL / db.url");
            user = requireConfigured(resolve("DB_USER", "db.user", props), "DB_USER / db.user");
            password = requireConfigured(resolve("DB_PASSWORD", "db.password", props), "DB_PASSWORD / db.password");
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать данные для базы данных", e);
        }
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к БД", e);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    private static String resolve(String envName, String propertyKey, Properties props) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return props.getProperty(propertyKey);
    }

    private static String requireConfigured(String value, String settingName) {
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Не задан параметр подключения к БД: " + settingName);
        }
        return value;
    }
}
