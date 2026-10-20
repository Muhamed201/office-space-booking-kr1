package ru.mirea.officebooking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.OpenDesk;
import ru.mirea.officebooking.model.PhoneBooth;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;

public class WorkspaceService {

    private static final String ENTITY_NAME = "Рабочее место";

    private final WorkspaceRepository workspaceRepository;
    private final BookingRepository bookingRepository;
    private final AccessPolicy accessPolicy;

    public WorkspaceService(WorkspaceRepository workspaceRepository, BookingRepository bookingRepository,
                            EmployeeRepository employeeRepository) {
        this.workspaceRepository = workspaceRepository;
        this.bookingRepository = bookingRepository;
        this.accessPolicy = new AccessPolicy(employeeRepository);
    }

    public Workspace createAs(long actorId, WorkspaceType type, String code, int floor, boolean hasMonitor,
                              Integer capacity) {
        accessPolicy.requireManager(actorId, "добавлять рабочие места");
        return create(type, code, floor, hasMonitor, capacity);
    }

    public Workspace updateAs(long actorId, long id, String code, int floor, boolean hasMonitor) {
        accessPolicy.requireManager(actorId, "изменять рабочие места");
        return update(id, code, floor, hasMonitor);
    }

    public Workspace changeStatusAs(long actorId, long id, WorkspaceStatus newStatus) {
        accessPolicy.requireManager(actorId, "менять статус рабочих мест");
        return changeStatus(id, newStatus);
    }

    public void deleteAs(long actorId, long id) {
        accessPolicy.requireManager(actorId, "удалять рабочие места");
        delete(id);
    }

    public List<Workspace> findAll() {
        return workspaceRepository.findAll();
    }

    public List<Workspace> findAvailableForBooking() {
        return workspaceRepository.findAll().stream().filter(Workspace::isAvailable).toList();
    }

    public Workspace findById(long id) {
        Validation.requirePositiveId(id, "id рабочего места");
        return workspaceRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId(ENTITY_NAME, id));
    }

    public Workspace create(WorkspaceType type, String code, int floor, boolean hasMonitor, Integer capacity) {
        Validation.requireNotNull(type, "Тип места");
        if (type != WorkspaceType.MEETING_ROOM && capacity != null) {
            throw new ValidationException("Вместимость задаётся только для переговорной");
        }
        Workspace candidate = switch (type) {
            case OPEN_DESK -> new OpenDesk(null, code, floor, WorkspaceStatus.AVAILABLE, hasMonitor);
            case PHONE_BOOTH -> new PhoneBooth(null, code, floor, WorkspaceStatus.AVAILABLE, hasMonitor);
            case MEETING_ROOM -> new MeetingRoom(null, code, floor, WorkspaceStatus.AVAILABLE, hasMonitor,
                    Validation.requireNotNull(capacity, "Вместимость переговорной"));
        };
        ensureCodeFree(candidate.getCode(), null);
        return workspaceRepository.save(candidate);
    }

    public Workspace update(long id, String code, int floor, boolean hasMonitor) {
        Workspace existing = findById(id);
        Workspace updated = existing.withDetails(code, floor, hasMonitor);
        ensureCodeFree(updated.getCode(), id);
        workspaceRepository.update(updated);
        return updated;
    }

    public Workspace changeStatus(long id, WorkspaceStatus newStatus) {
        Validation.requireNotNull(newStatus, "Новый статус");
        Workspace existing = findById(id);
        if (existing.getStatus() == newStatus) {
            throw new ValidationException("Рабочее место %s уже имеет статус %s".formatted(existing.getCode(), newStatus));
        }
        if (newStatus != WorkspaceStatus.AVAILABLE) {
            long futureActive = bookingRepository.countFutureActiveByWorkspaceId(id, LocalDateTime.now());
            if (futureActive > 0) {
                throw new ValidationException(
                        "Нельзя перевести место %s в статус %s: есть активные брони, которые ещё не закончились (%d)"
                                .formatted(existing.getCode(), newStatus, futureActive));
            }
        }
        Workspace updated = existing.withStatus(newStatus);
        workspaceRepository.update(updated);
        return updated;
    }

    public void delete(long id) {
        Workspace existing = findById(id);
        long bookings = bookingRepository.countByWorkspaceId(id);
        if (bookings > 0) {
            throw new ValidationException(
                    "Нельзя удалить рабочее место %s: на него оформлено броней — %d".formatted(existing.getCode(), bookings));
        }
        workspaceRepository.deleteById(id);
    }

    private void ensureCodeFree(String code, Long editingId) {
        Optional<Workspace> sameCode = workspaceRepository.findByCode(code);
        if (sameCode.isPresent() && !sameCode.get().getId().equals(editingId)) {
            throw new ValidationException("Рабочее место с кодом " + code + " уже существует");
        }
    }
}
