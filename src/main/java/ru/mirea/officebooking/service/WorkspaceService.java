package ru.mirea.officebooking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.Workspace;
import ru.mirea.officebooking.domain.WorkspaceStatus;
import ru.mirea.officebooking.domain.WorkspaceType;
import ru.mirea.officebooking.domain.BookingStatus;
import ru.mirea.officebooking.domain.EventType;
import ru.mirea.officebooking.error.ConflictException;
import ru.mirea.officebooking.repository.BookingRepository;
import java.time.LocalDateTime;
import ru.mirea.officebooking.dto.CreateWorkspaceRequest;
import ru.mirea.officebooking.dto.WorkspaceDto;
import ru.mirea.officebooking.error.EntityNotFoundException;
import ru.mirea.officebooking.error.ValidationException;
import ru.mirea.officebooking.repository.WorkspaceRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    private final BookingRepository bookings;
    private final AuditService audit;

    public WorkspaceService(WorkspaceRepository workspaceRepository, BookingRepository bookings, AuditService audit) {
        this.bookings = bookings;
        this.audit = audit;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> getAllWorkspaces() {
        return workspaceRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDto getWorkspaceById(UUID id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Рабочее место не найдено: " + id));
        return toDto(workspace);
    }

    public WorkspaceDto createWorkspace(CreateWorkspaceRequest request) {
        validateCapacity(request);
        if (workspaceRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Рабочее место с таким кодом уже существует: " + request.getCode());
        }

        Workspace workspace = new Workspace(
                request.getCode(),
                request.getFloor(),
                request.getType(),
                request.getStatus()
        );
        workspace.setHasMonitor(Boolean.TRUE.equals(request.getHasMonitor()));
        workspace.setCapacity(request.getCapacity());

        workspace = workspaceRepository.save(workspace);
        audit.logEvent(EventType.UPDATE, "Workspace", workspace.getId(), "Справочник мест обновлён");
        return toDto(workspace);
    }

    public WorkspaceDto updateWorkspace(UUID id, CreateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findLockedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Рабочее место не найдено: " + id));

        validateCapacity(request);
        if ((request.getStatus() != WorkspaceStatus.AVAILABLE && workspace.getStatus() == WorkspaceStatus.AVAILABLE)
                || workspace.getType() != request.getType()) {
            ensureNoBookings(id);
        }
        if (request.getCapacity() < workspace.getCapacity() &&
                bookings.existsByWorkspaceIdAndArchivedFalseAndStatusAndEndTimeAfterAndAttendeesGreaterThan(
                        id, BookingStatus.ACTIVE, LocalDateTime.now(BookingService.OFFICE_ZONE), request.getCapacity())) {
            throw new ConflictException("Новая вместимость меньше числа участников активной брони");
        }
        workspace.setCode(request.getCode());
        workspace.setFloor(request.getFloor());
        workspace.setType(request.getType());
        workspace.setStatus(request.getStatus());
        workspace.setHasMonitor(Boolean.TRUE.equals(request.getHasMonitor()));
        workspace.setCapacity(request.getCapacity());

        workspace = workspaceRepository.save(workspace);
        audit.logEvent(EventType.UPDATE, "Workspace", workspace.getId(), "Справочник мест обновлён");
        return toDto(workspace);
    }

    public void enableWorkspace(UUID id) {
        Workspace workspace = workspaceRepository.findLockedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Рабочее место не найдено: " + id));
        workspace.enable();
        audit.logEvent(EventType.UPDATE, "Workspace", id, "Рабочее место включено");
        workspaceRepository.save(workspace);
    }

    public void disableWorkspace(UUID id) {
        Workspace workspace = workspaceRepository.findLockedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Рабочее место не найдено: " + id));
        ensureNoBookings(id);
        workspace.disable();
        audit.logEvent(EventType.UPDATE, "Workspace", id, "Рабочее место отключено");
        workspaceRepository.save(workspace);
    }

    private void validateCapacity(CreateWorkspaceRequest request) {
        if (request.getType() != WorkspaceType.MEETING_ROOM && request.getCapacity() != 1) {
            throw new ValidationException("У стола и телефонной будки вместимость равна одному");
        }
    }

    private void ensureNoBookings(UUID id) {
        if (bookings.existsByWorkspaceIdAndArchivedFalseAndStatusAndEndTimeAfter(id, BookingStatus.ACTIVE, LocalDateTime.now(BookingService.OFFICE_ZONE))) {
            throw new ConflictException("На рабочее место есть активные брони");
        }
    }

    private WorkspaceDto toDto(Workspace workspace) {
        WorkspaceDto dto = new WorkspaceDto();
        dto.setId(workspace.getId());
        dto.setCode(workspace.getCode());
        dto.setFloor(workspace.getFloor());
        dto.setType(workspace.getType());
        dto.setStatus(workspace.getStatus());
        dto.setHasMonitor(workspace.getHasMonitor());
        dto.setCapacity(workspace.getCapacity());
        dto.setEnabled(workspace.getEnabled());
        dto.setCreatedAt(workspace.getCreatedAt());
        dto.setUpdatedAt(workspace.getUpdatedAt());
        return dto;
    }
}
