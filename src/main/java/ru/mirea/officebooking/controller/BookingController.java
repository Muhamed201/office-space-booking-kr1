package ru.mirea.officebooking.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.officebooking.dto.*;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.service.BookingService;
import ru.mirea.officebooking.service.BulkBookingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;
    private final BulkBookingService bulk;

    public BookingController(BookingService bookings, BulkBookingService bulk) {
        this.bookings = bookings;
        this.bulk = bulk;
    }

    @GetMapping
    public PagedModel<BookingDto> list(@Valid @ModelAttribute BookingFilter filter, Pageable pageable) {
        return new PagedModel<>(bookings.getAllBookings(filter, pageable));
    }

    @GetMapping("/{id}")
    public BookingDto get(@PathVariable UUID id) {
        return bookings.getBookingById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody CreateBookingRequest request) {
        return bookings.createBooking(request);
    }

    @PutMapping("/{id}")
    public BookingDto update(@PathVariable UUID id, @Valid @RequestBody UpdateBookingRequest request) {
        return bookings.updateBooking(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        bookings.archiveBooking(id);
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable UUID id) {
        bookings.restoreBooking(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentDelete(@PathVariable UUID id) {
        bookings.deleteBookingPermanently(id);
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@Valid @RequestBody BulkBookingService.Request request) {
        return bulk.apply(request);
    }
}
