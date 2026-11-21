package ru.mirea.officebooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.*;
import ru.mirea.officebooking.dto.*;
import ru.mirea.officebooking.error.*;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.filter.Pages;
import ru.mirea.officebooking.repository.*;
import ru.mirea.officebooking.security.CurrentUser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class BookingService {
    static final ZoneId OFFICE_ZONE = ZoneId.of("Europe/Moscow");
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "startTime", "attendees", "id");

    private final BookingRepository bookings;
    private final EmployeeRepository employees;
    private final WorkspaceRepository workspaces;
    private final TagRepository tags;
    private final AuditService audit;
    private final boolean permanentDelete;

    public BookingService(BookingRepository bookings, EmployeeRepository employees, WorkspaceRepository workspaces,
                          TagRepository tags, AuditService audit,
                          @Value("${app.security.permanent-delete-enabled}") boolean permanentDelete) {
        this.bookings = bookings;
        this.employees = employees;
        this.workspaces = workspaces;
        this.tags = tags;
        this.audit = audit;
        this.permanentDelete = permanentDelete;
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getAllBookings(BookingFilter filter, Pageable pageable) {
        return bookings.findAll(filter.specification(employeeScope()),
                Pages.sorted(pageable, SORT_FIELDS, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> exportBookings(BookingFilter filter, Sort sort) {
        var ordered = Pages.sorted(PageRequest.of(0, 100, sort), SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookings.findAll(filter.specification(employeeScope()), ordered.getSort()).stream().map(this::toDto).toList();
    }

    public UUID employeeScope() {
        var user = CurrentUser.get();
        return user.role() == Role.EMPLOYEE ? user.employeeId() : null;
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(UUID id) {
        return toDto(visibleBooking(id));
    }

    Booking visibleBooking(UUID id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> missing(id));
        UUID employee = employeeScope();
        if (employee != null && !booking.getEmployee().getId().equals(employee)) {
            throw missing(id);
        }
        return booking;
    }

    public BookingDto createBooking(CreateBookingRequest request) {
        return createBooking(request, null);
    }

    BookingDto createBooking(CreateBookingRequest request, String previewNumber) {
        UUID requestedEmployee = request.getEmployeeId();
        var actor = CurrentUser.get();
        if (actor.role() == Role.EMPLOYEE && requestedEmployee != null && !actor.employeeId().equals(requestedEmployee)) {
            throw new AccessDeniedException("Сотрудник может бронировать только от своего имени");
        }
        UUID employeeId = requestedEmployee == null ? actor.employeeId() : requestedEmployee;
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Сотрудник не найден"));
        Workspace workspace = lockedWorkspace(request.getWorkspaceId());
        validateReservation(employee, workspace, request.getStartTime(), request.getEndTime(), request.getAttendees(),
                LocalDateTime.now(OFFICE_ZONE));
        String number = previewNumber == null ? "BK-N" + bookings.nextBookingNumber() : previewNumber;
        Booking booking = new Booking(number, employee, workspace,
                request.getStartTime(), request.getEndTime(), BookingStatus.ACTIVE);
        setDetails(booking, request.getAttendees(), request.getPurpose(), request.getDescription(), request.getIsRecurring(),
                request.getRequiresEquipment());
        replaceTags(booking, request.getTags());
        booking = bookings.saveAndFlush(booking);
        log(EventType.CREATE, booking, "Создана бронь");
        return toDto(booking);
    }

    public BookingDto updateBooking(UUID id, UpdateBookingRequest request) {
        Booking booking = editableBooking(id);
        if (!booking.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Запись была изменена другим пользователем. Обновите данные");
        }
        LocalDateTime now = LocalDateTime.now(OFFICE_ZONE);
        boolean rescheduled = !booking.getWorkspace().getId().equals(request.getWorkspaceId())
                || !booking.getStartTime().equals(request.getStartTime())
                || !booking.getEndTime().equals(request.getEndTime());
        boolean reservationChanged = rescheduled || !Objects.equals(booking.getAttendees(), request.getAttendees());
        if (reservationChanged) {
            if (booking.getStatus() != BookingStatus.ACTIVE || !booking.getStartTime().isAfter(now)
                    || request.getStatus() != BookingStatus.ACTIVE) {
                throw new ValidationException("Время, место и участников можно менять у предстоящей активной брони");
            }
            Workspace workspace = lockedWorkspace(request.getWorkspaceId());
            validateReservation(booking.getEmployee(), workspace, request.getStartTime(), request.getEndTime(),
                    request.getAttendees(), now);
            booking.setWorkspace(workspace);
            booking.setStartTime(request.getStartTime());
            booking.setEndTime(request.getEndTime());
        }
        changeStatus(booking, request.getStatus(), now);
        setDetails(booking, request.getAttendees(), request.getPurpose(), request.getDescription(),
                request.getIsRecurring() == null ? booking.getIsRecurring() : request.getIsRecurring(),
                request.getRequiresEquipment() == null ? booking.getRequiresEquipment() : request.getRequiresEquipment());
        replaceTags(booking, request.getTags());
        bookings.flush();
        log(EventType.UPDATE, booking, "Обновлена бронь");
        return toDto(booking);
    }

    public void archiveBooking(UUID id) {
        requireManager();
        Booking booking = visibleBooking(id);
        if (!booking.getArchived()) {
            booking.archive();
            log(EventType.ARCHIVE, booking, "Бронь перемещена в архив");
        }
    }

    public void restoreBooking(UUID id) {
        requireManager();
        Booking booking = visibleBooking(id);
        if (booking.getArchived()) {
            if (booking.getStatus() == BookingStatus.ACTIVE && booking.getEndTime().isAfter(LocalDateTime.now(OFFICE_ZONE))) {
                Workspace workspace = lockedWorkspace(booking.getWorkspace().getId());
                validateAvailability(booking.getEmployee(), workspace, booking.getAttendees());
            }
            booking.restore();
            bookings.flush();
            log(EventType.RESTORE, booking, "Бронь восстановлена из архива");
        }
    }

    public void deleteBookingPermanently(UUID id) {
        if (CurrentUser.get().role() != Role.ADMIN || !permanentDelete) {
            throw new AccessDeniedException("Окончательное удаление недоступно");
        }
        Booking booking = visibleBooking(id);
        log(EventType.DELETE, booking, "Бронь удалена окончательно");
        bookings.delete(booking);
    }

    public void updateStatus(UUID id, BookingStatus status) {
        Booking booking = editableBooking(id);
        changeStatus(booking, status, LocalDateTime.now(OFFICE_ZONE));
        log(EventType.UPDATE, booking, "Изменён статус брони");
    }

    public void updateTag(UUID id, String name, boolean add) {
        Booking booking = editableBooking(id);
        String normalized = validateTag(name);
        if (add) {
            addTag(booking, normalized);
        } else {
            tags.findByName(normalized).ifPresent(booking::removeTag);
        }
        log(EventType.UPDATE, booking, "Обновлены теги брони");
    }

    private Booking editableBooking(UUID id) {
        requireManager();
        Booking booking = visibleBooking(id);
        if (booking.getArchived()) {
            throw new ConflictException("Сначала восстановите бронь из архива");
        }
        return booking;
    }

    void requireManager() {
        if (CurrentUser.get().role() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Операция доступна офис-менеджеру и администратору");
        }
    }

    private void changeStatus(Booking booking, BookingStatus status, LocalDateTime now) {
        if (booking.getStatus() == status) {
            return;
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new ValidationException("Статус завершённой, отменённой или пропущенной брони изменить нельзя");
        }
        boolean allowed = switch (status) {
            case CANCELLED -> now.isBefore(booking.getStartTime());
            case COMPLETED -> !now.isBefore(booking.getEndTime());
            case NO_SHOW -> !now.isBefore(booking.getStartTime());
            case ACTIVE -> false;
        };
        if (!allowed) {
            throw new ValidationException("Переход в выбранный статус невозможен для текущего времени");
        }
        booking.setStatus(status);
    }

    void validateReservation(Employee employee, Workspace workspace, LocalDateTime start, LocalDateTime end,
                             Integer attendees, LocalDateTime now) {
        validateAvailability(employee, workspace, attendees);
        if (!start.isBefore(end) || !start.toLocalDate().equals(end.toLocalDate())
                || !start.toLocalTime().equals(LocalTime.of(start.getHour(), 0))
                || !end.toLocalTime().equals(LocalTime.of(end.getHour(), 0))
                || start.getHour() < 8 || end.getHour() > 22
                || start.isBefore(now) || start.isAfter(now.plusDays(30))) {
            throw new ValidationException("Укажите целые часы в одни сутки с 08:00 до 22:00, не дальше 30 дней вперёд");
        }
        int limit = switch (workspace.getType()) {
            case OPEN_DESK -> 10;
            case MEETING_ROOM -> 4;
            case PHONE_BOOTH -> 1;
        };
        if (Duration.between(start, end).toHours() > limit) {
            throw new ValidationException("Для места " + workspace.getCode() + " максимальная длительность: " + limit + " ч.");
        }
    }

    private void validateAvailability(Employee employee, Workspace workspace, Integer attendees) {
        if (employee.getBlocked()) {
            throw new ValidationException("Сотрудник заблокирован");
        }
        if (!workspace.getEnabled() || workspace.getStatus() != WorkspaceStatus.AVAILABLE) {
            throw new ValidationException("Рабочее место недоступно");
        }
        if (workspace.getType() == WorkspaceType.MEETING_ROOM) {
            if (attendees == null || attendees < 1 || attendees > workspace.getCapacity()) {
                throw new ValidationException("Укажите число участников в пределах вместимости переговорной");
            }
        } else if (attendees != null) {
            throw new ValidationException("Число участников указывается для переговорной");
        }
    }

    private Workspace lockedWorkspace(UUID id) {
        return workspaces.findLockedById(id).orElseThrow(() -> new EntityNotFoundException("Рабочее место не найдено"));
    }

    private void setDetails(Booking booking, Integer attendees, String purpose, String description,
                            Boolean recurring, Boolean equipment) {
        booking.setAttendees(attendees);
        booking.setPurpose(purpose);
        booking.setDescription(description);
        booking.setIsRecurring(Boolean.TRUE.equals(recurring));
        booking.setRequiresEquipment(Boolean.TRUE.equals(equipment));
        booking.calculatePriority();
    }

    private void replaceTags(Booking booking, List<String> names) {
        if (names == null) {
            return;
        }
        Set<String> desired = new TreeSet<>();
        for (String name : names) {
            desired.add(validateTag(name));
        }
        List.copyOf(booking.getTags()).stream().filter(tag -> !desired.contains(tag.getName())).forEach(booking::removeTag);
        booking.getTags().forEach(tag -> desired.remove(tag.getName()));
        desired.forEach(name -> addTag(booking, name));
    }

    private String validateTag(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 50 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new ValidationException("Тег должен содержать от 1 до 50 символов");
        }
        return name.strip();
    }

    private void addTag(Booking booking, String name) {
        tags.ensureExists(name);
        booking.addTag(tags.findByName(name).orElseThrow());
    }

    private void log(EventType type, Booking booking, String message) {
        audit.logEvent(type, "Booking", booking.getId(), message + " " + booking.getBookingNumber());
    }

    private EntityNotFoundException missing(UUID id) {
        return new EntityNotFoundException("Бронирование не найдено: " + id);
    }

    private BookingDto toDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setBookingNumber(booking.getBookingNumber());
        dto.setEmployeeId(booking.getEmployee().getId());
        dto.setEmployeeName(booking.getEmployee().getFullName());
        dto.setWorkspaceId(booking.getWorkspace().getId());
        dto.setWorkspaceCode(booking.getWorkspace().getCode());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setStatus(booking.getStatus());
        dto.setAttendees(booking.getAttendees());
        dto.setPurpose(booking.getPurpose());
        dto.setDescription(booking.getDescription());
        dto.setIsRecurring(booking.getIsRecurring());
        dto.setRequiresEquipment(booking.getRequiresEquipment());
        dto.setPriority(booking.getPriority());
        dto.setTags(booking.getTags().stream().map(Tag::getName).sorted().toList());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setVersion(booking.getVersion());
        dto.setArchived(booking.getArchived());
        return dto;
    }
}
