package ru.mirea.officebooking.dto;

import ru.mirea.officebooking.domain.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookingDto {

    private UUID id;
    private String bookingNumber;
    private UUID employeeId;
    private String employeeName;
    private UUID workspaceId;
    private String workspaceCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private Integer attendees;
    private String purpose;

    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    private Boolean isRecurring;
    private Boolean requiresEquipment;
    private Integer priority;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private Boolean archived;

    public BookingDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public String getWorkspaceCode() { return workspaceCode; }
    public void setWorkspaceCode(String workspaceCode) { this.workspaceCode = workspaceCode; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Integer getAttendees() { return attendees; }
    public void setAttendees(Integer attendees) { this.attendees = attendees; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean recurring) { isRecurring = recurring; }
    public Boolean getRequiresEquipment() { return requiresEquipment; }
    public void setRequiresEquipment(Boolean requiresEquipment) { this.requiresEquipment = requiresEquipment; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
}
