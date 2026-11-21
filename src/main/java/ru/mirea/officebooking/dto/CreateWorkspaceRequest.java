package ru.mirea.officebooking.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;
import ru.mirea.officebooking.domain.WorkspaceStatus;
import ru.mirea.officebooking.domain.WorkspaceType;

public class CreateWorkspaceRequest {

    @NotBlank(message = "Код места обязателен")
    @Size(max = 20, message = "Код места не может превышать 20 символов")
    @Pattern(regexp = "[A-Z]{1,3}-[0-9]{1,3}", message = "Введите код в формате A-14")
    private String code;

    @NotNull(message = "Этаж обязателен")
    @Min(value = -2, message = "Этаж не может быть меньше -2")
    @Max(value = 50, message = "Этаж не может превышать 50")
    private Integer floor;

    @NotNull(message = "Тип места обязателен")
    private WorkspaceType type;

    @NotNull(message = "Статус обязателен")
    private WorkspaceStatus status;

    private Boolean hasMonitor = false;

    @NotNull(message = "Вместимость обязательна")
    @Min(value = 1, message = "Вместимость не может быть меньше 1")
    @Max(value = 100, message = "Вместимость не может превышать 100")
    private Integer capacity;

    public CreateWorkspaceRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public WorkspaceType getType() { return type; }
    public void setType(WorkspaceType type) { this.type = type; }
    public WorkspaceStatus getStatus() { return status; }
    public void setStatus(WorkspaceStatus status) { this.status = status; }
    public Boolean getHasMonitor() { return hasMonitor; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setHasMonitor(Boolean hasMonitor) { this.hasMonitor = hasMonitor; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
