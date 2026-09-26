package ru.mirea.officebooking.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import ru.mirea.officebooking.exception.BookingConflictException;
import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.exception.InvalidStatusTransitionException;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.exception.WorkspaceUnavailableException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;

public class
BookingService {

    public static final int BOOKING_HORIZON_DAYS = 30;
    public static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    public static final LocalTime WORKDAY_END = LocalTime.of(22, 0);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkspaceRepository workspaceRepository;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, EmployeeRepository employeeRepository,
                          WorkspaceRepository workspaceRepository) {
        this(bookingRepository, employeeRepository, workspaceRepository, Clock.systemDefaultZone());
    }

    public BookingService(BookingRepository bookingRepository, EmployeeRepository employeeRepository,
                          WorkspaceRepository workspaceRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.workspaceRepository = workspaceRepository;
        this.clock = clock;
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(long id) {
        Validation.requirePositiveId(id, "id брони");
        return bookingRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId("Бронь", id));
    }

    public Booking create(long employeeId, long workspaceId, LocalDateTime start, LocalDateTime end,
                          Integer attendees, String purpose) {
        Validation.requirePositiveId(employeeId, "id сотрудника");
        Validation.requirePositiveId(workspaceId, "id рабочего места");
        Validation.requireNotNull(start, "Начало брони");
        Validation.requireNotNull(end, "Конец брони");
        Booking.validateAttendees(attendees);
        Booking.normalizePurpose(purpose);

        Employee employee = loadEmployee(employeeId);
        Workspace workspace = loadWorkspace(workspaceId);

        if (employee.isBlocked()) {
            throw new ValidationException(
                    "Сотрудник %s заблокирован и не может создавать брони".formatted(employee.getFullName()));
        }
        ensureAvailable(workspace);
        validateInterval(start, end, workspace, LocalDateTime.now(clock));

        Booking candidate = new Booking(employeeId, workspaceId, start, end, attendees, purpose);
        workspace.validateBooking(candidate);
        checkNoConflicts(workspace, employee, start, end, null);

        return bookingRepository.save(candidate);
    }

    public Booking updateTime(long bookingId, LocalDateTime newStart, LocalDateTime newEnd,
                              Integer attendees, String purpose) {
        Validation.requireNotNull(newStart, "Начало брони");
        Validation.requireNotNull(newEnd, "Конец брони");
        Booking.validateAttendees(attendees);
        Booking.normalizePurpose(purpose);

        Booking existing = findById(bookingId);
        if (!existing.isActive()) {
            throw new ValidationException(
                    "Изменять можно только активную бронь, текущий статус брони #%d: %s".formatted(bookingId, existing.getStatus()));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (existing.getStartTime().isBefore(now)) {
            throw new ValidationException("Нельзя изменить бронь #%d: она уже началась".formatted(bookingId));
        }

        Workspace workspace = loadWorkspace(existing.getWorkspaceId());
        Employee employee = loadEmployee(existing.getEmployeeId());
        ensureAvailable(workspace);
        validateInterval(newStart, newEnd, workspace, now);

        Booking candidate = existing.withDetails(newStart, newEnd, attendees, purpose);
        workspace.validateBooking(candidate);
        checkNoConflicts(workspace, employee, newStart, newEnd, bookingId);

        bookingRepository.update(candidate);
        return candidate;
    }

    public Booking changeStatus(long bookingId, BookingStatus target) {
        Validation.requireNotNull(target, "Новый статус");
        Booking booking = findById(bookingId);
        BookingStatus current = booking.getStatus();
        if (current.isFinal()) {
            throw new InvalidStatusTransitionException(
                    "Бронь #%d уже в конечном статусе %s, статус менять нельзя".formatted(bookingId, current));
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Нельзя перевести бронь #%d из статуса %s в %s".formatted(bookingId, current, target));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        switch (target) {
            case CANCELLED -> {
                if (!now.isBefore(booking.getStartTime())) {
                    throw new InvalidStatusTransitionException(
                            "Нельзя отменить бронь #%d: она уже началась (начало %s)"
                                    .formatted(bookingId, booking.getStartTime().format(TIME_FORMAT)));
                }
            }
            case COMPLETED -> {
                if (now.isBefore(booking.getEndTime())) {
                    throw new InvalidStatusTransitionException(
                            "Нельзя завершить бронь #%d: она ещё не закончилась (конец %s)"
                                    .formatted(bookingId, booking.getEndTime().format(TIME_FORMAT)));
                }
            }
            case NO_SHOW -> {
                if (now.isBefore(booking.getStartTime())) {
                    throw new InvalidStatusTransitionException(
                            "Нельзя отметить неявку по брони #%d: она ещё не началась (начало %s)"
                                    .formatted(bookingId, booking.getStartTime().format(TIME_FORMAT)));
                }
            }
            default -> throw new InvalidStatusTransitionException("Недопустимый целевой статус: " + target);
        }
        Booking updated = booking.withStatus(target);
        bookingRepository.update(updated);
        return updated;
    }

    public Booking cancel(long bookingId) {
        return changeStatus(bookingId, BookingStatus.CANCELLED);
    }

    public void delete(long bookingId) {
        findById(bookingId);
        bookingRepository.deleteById(bookingId);
    }

    private Employee loadEmployee(long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> EntityNotFoundException.forId("Сотрудник", employeeId));
    }

    private Workspace loadWorkspace(long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> EntityNotFoundException.forId("Рабочее место", workspaceId));
    }

    private void ensureAvailable(Workspace workspace) {
        if (!workspace.isAvailable()) {
            throw new WorkspaceUnavailableException(
                    "Рабочее место %s недоступно для бронирования (статус %s)".formatted(workspace.getCode(), workspace.getStatus()));
        }
    }

    private void validateInterval(LocalDateTime start, LocalDateTime end, Workspace workspace, LocalDateTime now) {
        if (!isWholeHour(start) || !isWholeHour(end)) {
            throw new ValidationException("Время брони должно быть кратно часу (например, 10:00), бронирование идёт с шагом в 1 час");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Начало брони должно быть раньше конца");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new ValidationException("Бронь должна начинаться и заканчиваться в одни календарные сутки");
        }
        if (start.toLocalTime().isBefore(WORKDAY_START) || end.toLocalTime().isAfter(WORKDAY_END)) {
            throw new ValidationException(
                    "Бронь должна укладываться в рабочий день %s–%s".formatted(WORKDAY_START, WORKDAY_END));
        }
        if (start.isBefore(now)) {
            throw new ValidationException("Начало брони не может быть в прошлом");
        }
        if (start.isAfter(now.plusDays(BOOKING_HORIZON_DAYS))) {
            throw new ValidationException(
                    "Бронь можно создать не более чем за %d дней вперёд".formatted(BOOKING_HORIZON_DAYS));
        }
        long hours = Duration.between(start, end).toHours();
        if (hours > workspace.maxBookingHours()) {
            throw new ValidationException(
                    "Длительность брони %d ч. превышает предел %d ч. для места %s (%s)"
                            .formatted(hours, workspace.maxBookingHours(), workspace.getCode(), workspace.type()));
        }
    }

    private static boolean isWholeHour(LocalDateTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private void checkNoConflicts(Workspace workspace, Employee employee, LocalDateTime start, LocalDateTime end,
                                  Long excludeBookingId) {
        List<Booking> workspaceConflicts = bookingRepository.findActiveOverlappingForWorkspace(
                workspace.getId(), start, end, excludeBookingId);
        if (!workspaceConflicts.isEmpty()) {
            throw new BookingConflictException(
                    "Место %s уже занято на это время: %s".formatted(workspace.getCode(), describe(workspaceConflicts.get(0))));
        }
        List<Booking> employeeConflicts = bookingRepository.findActiveOverlappingForEmployee(
                employee.getId(), start, end, excludeBookingId);
        if (!employeeConflicts.isEmpty()) {
            throw new BookingConflictException(
                    "У сотрудника %s уже есть бронь на это время: %s"
                            .formatted(employee.getFullName(), describe(employeeConflicts.get(0))));
        }
    }

    private static String describe(Booking booking) {
        return "бронь #%d (%s — %s)".formatted(booking.getId(),
                booking.getStartTime().format(TIME_FORMAT), booking.getEndTime().format(TIME_FORMAT));
    }
}
