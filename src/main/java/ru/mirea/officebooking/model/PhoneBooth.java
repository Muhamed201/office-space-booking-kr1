package ru.mirea.officebooking.model;

public class PhoneBooth extends Workspace {
    private int capacity;

    public PhoneBooth() {
        super();
        setType(WorkspaceType.PHONE_BOOTH);
    }

    public PhoneBooth(Long id, String name, WorkspaceStatus status, int capacity) {
        super(id, name, WorkspaceType.PHONE_BOOTH, status);
        this.capacity = capacity;
    }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public boolean hasCapacity(int people) {
        return people <= capacity;
    }
    
    @Override
    public String toString() {
        return super.toString() + String.format(" [Capacity: %d]", capacity);
    }
}