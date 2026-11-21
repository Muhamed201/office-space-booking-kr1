package ru.mirea.officebooking.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class FlywayHistoryConfig {
    @Bean
    FlywayMigrationStrategy migrateBookingSchema() {
        return flyway -> {
            var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            boolean historyExists = Boolean.TRUE.equals(jdbc.queryForObject(
                    "select to_regclass('public.flyway_schema_history') is not null", Boolean.class));
            boolean draft = historyExists && Boolean.TRUE.equals(jdbc.queryForObject(
                    "select exists (select 1 from flyway_schema_history where version = '4' " +
                            "and description = 'create user account' and success)", Boolean.class));
            if (draft) {
                Flyway.configure().configuration(flyway.getConfiguration())
                        .locations("classpath:db/kr3-draft", "classpath:db/common")
                        .load().migrate();
            } else {
                flyway.migrate();
            }
        };
    }
}
