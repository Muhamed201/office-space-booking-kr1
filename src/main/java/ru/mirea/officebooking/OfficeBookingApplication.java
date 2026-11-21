package ru.mirea.officebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Главное приложение Office Booking Backend.
 *
 * @SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class) включает:
 * - @Configuration — класс содержит конфигурацию Spring
 * - @EnableAutoConfiguration — Spring автоматически настраивает компоненты
 * - @ComponentScan — поиск компонентов в текущем пакете и подпакетах
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OfficeBookingApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        SpringApplication.run(OfficeBookingApplication.class, args);
    }
}
