package ru.mirea.officebooking.ui;

import ru.mirea.officebooking.model.*;
import ru.mirea.officebooking.service.WorkspaceService;

import java.util.List;

public class WorkspaceMenu {
    private final WorkspaceService workspaceService;
    private final ConsoleReader reader; // Класс Мухамеда

    public WorkspaceMenu(WorkspaceService workspaceService, ConsoleReader reader) {
        this.workspaceService = workspaceService;
        this.reader = reader;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Меню рабочих мест ===");
            System.out.println("1. Показать все рабочие места");
            System.out.println("2. Создать Open Desk");
            System.out.println("3. Создать Meeting Room");
            System.out.println("4. Создать Phone Booth");
            System.out.println("5. Проверить доступность");
            System.out.println("6. Проверить вместимость");
            System.out.println("7. Удалить рабочее место");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            int choice = reader.readInt();
            switch (choice) {
                case 1 -> showAll();
                case 2 -> createOpenDesk();
                case 3 -> createMeetingRoom();
                case 4 -> createPhoneBooth();
                case 5 -> checkAvailability();
                case 6 -> checkCapacity();
                case 7 -> deleteWorkspace();
                case 0 -> running = false;
                default -> System.out.println("Неверный ввод.");
            }
        }
    }

    private void showAll() {
        List<Workspace> workspaces = workspaceService.getAll();
        if (workspaces.isEmpty()) {
            System.out.println("Список рабочих мест пуст.");
            return;
        }
        workspaces.forEach(System.out::println);
    }

    private void createOpenDesk() {
        System.out.print("Введите название (например, Стол №5): ");
        String name = reader.readString();
        OpenDesk desk = new OpenDesk(null, name, WorkspaceStatus.AVAILABLE);
        workspaceService.create(desk);
        System.out.println("Open Desk успешно создан!");
    }

    private void createMeetingRoom() {
        System.out.print("Введите название переговорки: ");
        String name = reader.readString();
        System.out.print("Введите вместимость: ");
        int capacity = reader.readInt();
        MeetingRoom room = new MeetingRoom(null, name, WorkspaceStatus.AVAILABLE, capacity);
        workspaceService.create(room);
        System.out.println("Meeting Room успешно создана!");
    }

    private void createPhoneBooth() {
        System.out.print("Введите название будки: ");
        String name = reader.readString();
        System.out.print("Введите вместимость: ");
        int capacity = reader.readInt();
        PhoneBooth booth = new PhoneBooth(null, name, WorkspaceStatus.AVAILABLE, capacity);
        workspaceService.create(booth);
        System.out.println("Phone Booth успешно создана!");
    }

    private void checkAvailability() {
        System.out.print("Введите ID рабочего места: ");
        Long id = reader.readLong();
        boolean available = workspaceService.isAvailable(id);
        System.out.println("Статус: " + (available ? "Доступно" : "Занято/Недоступно"));
    }

    private void checkCapacity() {
        System.out.print("Введите ID рабочего места: ");
        Long id = reader.readLong();
        System.out.print("Введите количество человек: ");
        int people = reader.readInt();
        boolean fits = workspaceService.checkCapacity(id, people);
        System.out.println("Результат: " + (fits ? "Вмещает" : "Не вмещает"));
    }

    private void deleteWorkspace() {
        System.out.print("Введите ID для удаления: ");
        Long id = reader.readLong();
        workspaceService.delete(id);
        System.out.println("Рабочее место удалено.");
    }
}