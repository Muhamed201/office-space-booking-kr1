package ru.mirea.officebooking.ui;

import java.time.LocalDateTime;
import java.util.List;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.service.BookingService;

public class BookingMenu {

    private static final List<BookingStatus> TARGET_STATUSES =
            List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);

    private final BookingService bookingService;
    private final ConsoleReader reader;
    private final BookingPrinter printer;

    public BookingMenu(BookingService bookingService, BookingPrinter printer, ConsoleReader reader) {
        this.bookingService = bookingService;
        this.printer = printer;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Бронирования ---
                    1. Список всех
                    2. Найти по id
                    3. Создать
                    4. Изменить время / участников / цель
                    5. Сменить статус (отмена, завершение, неявка)
                    6. Удалить
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> printer.printSorted(bookingService.findAll());
                    case 2 -> findById();
                    case 3 -> create();
                    case 4 -> updateTime();
                    case 5 -> changeStatus();
                    case 6 -> delete();
                    case 0 -> inMenu = false;
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void findById() {
        System.out.println(bookingService.findById(reader.readId("id брони: ")));
    }

    private void create() {
        System.out.println("Бронирование идёт с шагом в 1 час, с 08:00 до 22:00, не более чем на 30 дней вперёд.");
        long employeeId = reader.readId("id сотрудника: ");
        long workspaceId = reader.readId("id места: ");
        LocalDateTime start = reader.readDateTime("Начало");
        LocalDateTime end = reader.readDateTime("Конец");
        Integer attendees = reader.readOptionalPositiveInt("Число участников (только для переговорной, Enter — пропустить): ");
        String purpose = reader.readUntilValid(
                () -> reader.readLine("Цель (Enter — пропустить): "), Booking::normalizePurpose);
        Booking created = bookingService.create(employeeId, workspaceId, start, end, attendees, purpose);
        System.out.println("Создана: " + created);
    }

    private void updateTime() {
        long id = reader.readId("id брони: ");
        System.out.println("Текущие данные: " + bookingService.findById(id));
        LocalDateTime start = reader.readDateTime("Новое начало");
        LocalDateTime end = reader.readDateTime("Новый конец");
        Integer attendees = reader.readOptionalPositiveInt("Число участников (Enter — очистить): ");
        String purpose = reader.readUntilValid(
                () -> reader.readLine("Новая цель (Enter — очистить): "), Booking::normalizePurpose);
        System.out.println("Обновлена: " + bookingService.updateTime(id, start, end, attendees, purpose));
    }

    private void changeStatus() {
        long id = reader.readId("id брони: ");
        System.out.println("Текущие данные: " + bookingService.findById(id));
        BookingStatus target = reader.readChoice("Новый статус:", TARGET_STATUSES);
        System.out.println("Обновлена: " + bookingService.changeStatus(id, target));
    }

    private void delete() {
        long id = reader.readId("id брони: ");
        System.out.println(bookingService.findById(id));
        if (!reader.readYesNo("Удалить бронь безвозвратно?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        bookingService.delete(id);
        System.out.println("Бронь id=" + id + " удалена.");
    }
}
