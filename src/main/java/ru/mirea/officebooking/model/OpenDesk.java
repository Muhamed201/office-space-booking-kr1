package ru.mirea.officebooking.model;

public class OpenDesk extends Workspace {
    public OpenDesk() {
        super();
        setType(WorkspaceType.OPEN_DESK);
    }

    public OpenDesk(Long id, String name, WorkspaceStatus status) {
        super(id, name, WorkspaceType.OPEN_DESK, status);
    }

    @Override
    public boolean hasCapacity(int people) {
        return people <= 1; // Open Desk только для одного
    }
}