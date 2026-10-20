package ru.mirea.officebooking.service;

import java.util.List;
import ru.mirea.officebooking.dto.BookingFilter;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.dto.BookingSort;
import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.repository.BookingQueryRepository;

public class BookingQueryService {

    private final BookingQueryRepository queryRepository;

    public BookingQueryService(BookingQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public List<BookingRow> search(BookingFilter filter, BookingSort sort, boolean ascending) {
        Validation.requireNotNull(filter, "Фильтр броней");
        Validation.requireNotNull(sort, "Параметр сортировки");
        return queryRepository.search(filter, sort, ascending);
    }

    public List<BookingRow> findAll() {
        return search(BookingFilter.empty(), BookingSort.START_TIME, false);
    }

    public BookingRow getRow(long bookingId) {
        Validation.requirePositiveId(bookingId, "id брони");
        return queryRepository.findRowById(bookingId)
                .orElseThrow(() -> EntityNotFoundException.forId("Бронь", bookingId));
    }

    public List<Integer> availableFloors() {
        return queryRepository.distinctFloors();
    }
}
