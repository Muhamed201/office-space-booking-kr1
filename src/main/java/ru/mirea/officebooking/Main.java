package ru.mirea.officebooking;

/**
 * Точка входа консольного приложения.
 *
 * Что здесь должно происходить (подробности — docs/03-architecture.md):
 *   1. применить миграции Flyway  — Flyway.configure().dataSource(url, user, pass).load().migrate()
 *      (параметры из src/main/resources/application.properties, см. docs/07-migrations.md)
 *   2. вручную собрать граф объектов:
 *      DatabaseManager -> *Repository -> *Service -> *Menu
 *   3. запустить цикл главного меню (docs/05-menu-and-features.md), пока не выбран пункт «0».
 *
 * Здесь НЕ должно быть SQL и бизнес-логики.
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("СИСТЕМА УПРАВЛЕНИЯ ОФИСНЫМИ МЕСТАМИ");
        // TODO: миграции, сборка объектов, запуск ConsoleApp.
    }
}
