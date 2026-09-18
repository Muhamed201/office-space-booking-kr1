package ru.mirea.officebooking.model;

public abstract class Workspace {
    private Long id;
    private String name;
    private WorkspaceType type;
    private WorkspaceStatus status;

    public Workspace() {}

    public Workspace(Long id, String name, WorkspaceType type, WorkspaceStatus status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WorkspaceType getType() { return type; }
    public void setType(WorkspaceType type) { this.type = type; }
    public WorkspaceStatus getStatus() { return status; }
    public void setStatus(WorkspaceStatus status) { this.status = status; }

    // Абстрактный метод (твое требование из тикета — полиморфизм)
    public abstract boolean hasCapacity(int people);
    
    @Override
    public String toString() {
        return String.format("[%s] %s (ID: %d, Status: %s)", type, name, id, status);
    }
}