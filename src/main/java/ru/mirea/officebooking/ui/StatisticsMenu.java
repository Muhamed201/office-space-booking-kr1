package ru.mirea.officebooking.ui;

import java.util.Map;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.service.StatisticsService;

public class StatisticsMenu {

    private static final int TOP_SIZE = 5;

    private final StatisticsService statisticsService;
    private final ConsoleReader reader;

    public StatisticsMenu(StatisticsService statisticsService, ConsoleReader reader) {
        this.statisticsService = statisticsService;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Статистика ---
                    1. Показать сводку
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> printSummary();
                    case 0 -> inMenu = false;
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printSummary() {
        System.out.println();
        System.out.printf("Всего сотрудников: %d (заблокировано: %d)%n",
                statisticsService.totalEmployees(), statisticsService.blockedEmployees());

        Map<WorkspaceType, Long> byType = statisticsService.workspacesByType();
        Map<WorkspaceStatus, Long> byStatus = statisticsService.workspacesByStatus();
        long totalWorkspaces = byType.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("Всего рабочих мест: %d (доступно: %d, на обслуживании: %d, выведено из эксплуатации: %d)%n",
                totalWorkspaces,
                byStatus.get(WorkspaceStatus.AVAILABLE),
                byStatus.get(WorkspaceStatus.MAINTENANCE),
                byStatus.get(WorkspaceStatus.DECOMMISSIONED));
        byType.forEach((type, count) -> System.out.printf("  %s: %d%n", type, count));

        Map<BookingStatus, Long> byBookingStatus = statisticsService.bookingsByStatus();
        long totalBookings = byBookingStatus.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("Бронирований: %d | активных: %d | завершённых: %d | отменённых: %d | неявок: %d%n",
                totalBookings,
                byBookingStatus.get(BookingStatus.ACTIVE),
                byBookingStatus.get(BookingStatus.COMPLETED),
                byBookingStatus.get(BookingStatus.CANCELLED),
                byBookingStatus.get(BookingStatus.NO_SHOW));

        System.out.printf("Средняя длительность завершённой брони: %.1f ч%n",
                statisticsService.averageDurationHoursCompleted());
        System.out.printf("Доля неявок: %.0f%%%n", statisticsService.noShowRate() * 100);

        System.out.println("Топ-" + TOP_SIZE + " самых бронируемых мест:");
        statisticsService.topWorkspaces(TOP_SIZE)
                .forEach(item -> System.out.printf("  %s — %d брон.%n", item.label(), item.count()));

        System.out.println("Топ-" + TOP_SIZE + " самых активных сотрудников:");
        statisticsService.topEmployees(TOP_SIZE)
                .forEach(item -> System.out.printf("  %s — %d брон.%n", item.label(), item.count()));

        StatisticsService.NamedCount floor = statisticsService.busiestFloor();
        System.out.printf("Самый загруженный этаж: %s (%d брон.)%n", floor.label(), floor.count());
    }
}
