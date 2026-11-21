package ru.mirea.officebooking.dto;

import ru.mirea.officebooking.domain.WorkspaceStatus;
import ru.mirea.officebooking.domain.WorkspaceType;

import java.time.LocalDateTime;
import java.util.UUID;

public class WorkspaceDto {

    private UUID id;
    private String code;
    private Integer floor;
    private WorkspaceType type;
    private WorkspaceStatus status;
    private Boolean hasMonitor;
    private Integer capacity;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkspaceDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public WorkspaceType getType() { return type; }
    public void setType(WorkspaceType type) { this.type = type; }
    public WorkspaceStatus getStatus() { return status; }
    public void setStatus(WorkspaceStatus status) { this.status = status; }
    public Boolean getHasMonitor() { return hasMonitor; }
    public void setHasMonitor(Boolean hasMonitor) { this.hasMonitor = hasMonitor; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
