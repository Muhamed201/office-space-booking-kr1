package ru.mirea.officebooking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.Booking;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.dto.AnalyticsResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public AnalyticsService(BookingRepository bookingRepository, BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    public AnalyticsResponse getSummary() {
        var employee = bookingService.employeeScope();
        List<Booking> allBookings = employee == null ? bookingRepository.findAll() :
                bookingRepository.findAll((root, query, cb) -> cb.equal(root.get("employee").get("id"), employee));

        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalBookings(allBookings.size());
        response.setActiveBookings(allBookings.stream().filter(b -> b.getStatus() == ru.mirea.officebooking.domain.BookingStatus.ACTIVE).count());
        response.setArchivedBookings(allBookings.stream().filter(Booking::getArchived).count());
        response.setRecurringBookings(allBookings.stream().filter(Booking::getIsRecurring).count());
        response.setAverageAttendees(allBookings.stream()
                .filter(b -> b.getAttendees() != null)
                .mapToInt(Booking::getAttendees)
                .average()
                .orElse(0.0));

        Map<String, Long> byWorkspace = new HashMap<>();
        Map<String, Long> byStatus = new HashMap<>();
        Map<Integer, Long> byPriority = new HashMap<>();

        for (Booking b : allBookings) {
            String workspaceCode = b.getWorkspace().getCode();
            byWorkspace.merge(workspaceCode, 1L, Long::sum);

            String status = b.getStatus().name();
            byStatus.merge(status, 1L, Long::sum);

            Integer priority = b.getPriority();
            byPriority.merge(priority, 1L, Long::sum);
        }

        response.setByWorkspace(byWorkspace);
        response.setByStatus(byStatus);
        response.setByPriority(byPriority);

        return response;
    }
}
