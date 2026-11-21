package ru.mirea.officebooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CreateBookingRequest {


    private UUID employeeId;

    @NotNull(message = "ID рабочего места обязателен")
    private UUID workspaceId;

    @NotNull(message = "Время начала обязательно")
    @JsonFormat(lenient = OptBoolean.FALSE)
    private LocalDateTime startTime;

    @NotNull(message = "Время окончания обязательно")
    @JsonFormat(lenient = OptBoolean.FALSE)
    private LocalDateTime endTime;

    @Min(value = 1, message = "Количество участников не может быть меньше 1")
    private Integer attendees;

    @Size(max = 500, message = "Цель не может превышать 500 символов")
    @Pattern(regexp = "[\\P{Cc}\\t\\r\\n]*", message = "Цель содержит недопустимые символы")
    private String purpose;

    @Size(max = 2000)
    @Pattern(regexp = "[\\P{Cc}\\t\\r\\n]*", message = "Описание содержит недопустимые символы")
    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private Boolean isRecurring = false;
    private Boolean requiresEquipment = false;
    private List<@NotBlank @Size(max = 50) String> tags;

    public CreateBookingRequest() {}

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getAttendees() { return attendees; }
    public void setAttendees(Integer attendees) { this.attendees = attendees; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Boolean getIsRecurring() { return isRecurring; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setIsRecurring(Boolean recurring) { isRecurring = recurring; }
    public Boolean getRequiresEquipment() { return requiresEquipment; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setRequiresEquipment(Boolean requiresEquipment) { this.requiresEquipment = requiresEquipment; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
