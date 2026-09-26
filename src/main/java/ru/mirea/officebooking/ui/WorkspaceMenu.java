package ru.mirea.officebooking.ui;

import java.util.List;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.service.WorkspaceService;

public class WorkspaceMenu {

    private final WorkspaceService workspaceService;
    private final ConsoleReader reader;

    public WorkspaceMenu(WorkspaceService workspaceService, ConsoleReader reader) {
        this.workspaceService = workspaceService;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Рабочие места ---
                    1. Список всех
                    2. Найти по id
                    3. Добавить
                    4. Изменить (код, этаж, монитор)
                    5. Сменить статус
                    6. Удалить
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> listAll();
                    case 2 -> findById();
                    case 3 -> create();
                    case 4 -> update();
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

    private void listAll() {
        List<Workspace> workspaces = workspaceService.findAll();
        if (workspaces.isEmpty()) {
            System.out.println("Рабочих мест пока нет.");
            return;
        }
        workspaces.forEach(System.out::println);
    }

    private void findById() {
        System.out.println(workspaceService.findById(reader.readId("id места: ")));
    }

    private void create() {
        WorkspaceType type = reader.readEnum(WorkspaceType.class, "Тип места:");
        String code = reader.readUntilValid(() -> reader.readLine("Код (например, A-14): "), Workspace::normalizeCode);
        int floor = reader.readUntilValid(() -> reader.readInt("Этаж: "), Workspace::validateFloor);
        boolean hasMonitor = reader.readYesNo("Есть монитор?");
        Integer capacity = null;
        if (type == WorkspaceType.MEETING_ROOM) {
            capacity = reader.readUntilValid(() -> reader.readInt("Вместимость: "), MeetingRoom::validateCapacity);
        }
        System.out.println("Создано: " + workspaceService.create(type, code, floor, hasMonitor, capacity));
    }

    private void update() {
        long id = reader.readId("id места: ");
        System.out.println("Текущие данные: " + workspaceService.findById(id));
        String code = reader.readUntilValid(() -> reader.readLine("Новый код: "), Workspace::normalizeCode);
        int floor = reader.readUntilValid(() -> reader.readInt("Новый этаж: "), Workspace::validateFloor);
        boolean hasMonitor = reader.readYesNo("Есть монитор?");
        System.out.println("Обновлено: " + workspaceService.update(id, code, floor, hasMonitor));
    }

    private void changeStatus() {
        long id = reader.readId("id места: ");
        System.out.println("Текущие данные: " + workspaceService.findById(id));
        WorkspaceStatus status = reader.readEnum(WorkspaceStatus.class, "Новый статус:");
        System.out.println("Обновлено: " + workspaceService.changeStatus(id, status));
    }

    private void delete() {
        long id = reader.readId("id места: ");
        Workspace workspace = workspaceService.findById(id);
        if (!reader.readYesNo("Удалить рабочее место " + workspace.getCode() + "?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        workspaceService.delete(id);
        System.out.println("Место id=" + id + " удалено.");
    }
}
