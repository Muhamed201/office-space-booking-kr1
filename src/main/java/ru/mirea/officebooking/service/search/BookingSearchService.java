package ru.mirea.officebooking.service.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.repository.BookingRepository;

public class BookingSearchService {

    private final BookingRepository bookingRepository;

    public BookingSearchService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> searchByEmployee(String query) {
        String text = Validation.requireText(query, "Строка поиска", Employee.MAX_FULL_NAME_LENGTH);
        return bookingRepository.searchByEmployeeNameOrEmail(text);
    }

    public List<Booking> searchByWorkspaceCode(String query) {
        String text = Validation.requireText(query, "Строка поиска", Workspace.MAX_CODE_LENGTH);
        return bookingRepository.searchByWorkspaceCode(text);
    }

    public List<Booking> searchByDate(LocalDate day) {
        Validation.requireNotNull(day, "Дата");
        return bookingRepository.searchByDate(day);
    }

    public List<Booking> filterByStatus(BookingStatus status) {
        Validation.requireNotNull(status, "Статус брони");
        return bookingRepository.filterByStatus(status);
    }

    public List<Booking> filterByWorkspaceType(WorkspaceType type) {
        Validation.requireNotNull(type, "Тип места");
        return bookingRepository.filterByWorkspaceType(type);
    }

    public List<Booking> filterByFloor(int floor) {
        Workspace.validateFloor(floor);
        return bookingRepository.filterByFloor(floor);
    }

    public List<Booking> filterByDateRange(LocalDateTime from, LocalDateTime to) {
        Validation.requireNotNull(from, "Начало диапазона");
        Validation.requireNotNull(to, "Конец диапазона");
        if (from.isAfter(to)) {
            throw new ValidationException("Начало диапазона не может быть позже его конца");
        }
        return bookingRepository.filterByDateRange(from, to);
    }
}
