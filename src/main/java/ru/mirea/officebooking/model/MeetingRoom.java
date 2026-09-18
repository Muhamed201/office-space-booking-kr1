package ru.mirea.officebooking.model;

public class MeetingRoom extends Workspace {
    private int capacity;

    public MeetingRoom() {
        super();
        setType(WorkspaceType.MEETING_ROOM);
    }

    public MeetingRoom(Long id, String name, WorkspaceStatus status, int capacity) {
        super(id, name, WorkspaceType.MEETING_ROOM, status);
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