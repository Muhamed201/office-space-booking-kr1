package ru.mirea.officebooking.filter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.sqm.TemporalUnit;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.officebooking.domain.Booking;
import ru.mirea.officebooking.domain.BookingStatus;
import ru.mirea.officebooking.error.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public record BookingFilter(@Size(max = 200) String q, UUID workspaceId, BookingStatus status,
                            Boolean isRecurring, Boolean requiresEquipment, @Min(1) Integer priority,
                            @Size(max = 50) String tag, Boolean archived, @Min(1) Integer minAttendees,
                            @Min(0) Long minDuration, @Min(0) Long maxDuration) {

    public Specification<Booking> specification(UUID employeeId) {
        if (minDuration != null && maxDuration != null && minDuration > maxDuration) {
            throw new ValidationException("Минимальная длительность превышает максимальную");
        }
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (employeeId != null) predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            if (q != null && !q.isBlank()) {
                String pattern = contains(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("purpose")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.get("employee").get("fullName")), pattern, '\\')));
            }
            if (workspaceId != null) predicates.add(cb.equal(root.get("workspace").get("id"), workspaceId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (isRecurring != null) predicates.add(cb.equal(root.get("isRecurring"), isRecurring));
            if (requiresEquipment != null) predicates.add(cb.equal(root.get("requiresEquipment"), requiresEquipment));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            if (tag != null && !tag.isBlank()) predicates.add(cb.equal(root.join("tags").get("name"), tag.trim()));
            if (archived != null) predicates.add(cb.equal(root.get("archived"), archived));
            if (minAttendees != null) predicates.add(cb.ge(root.get("attendees"), minAttendees));
            if (minDuration != null || maxDuration != null) {
                var hibernate = (HibernateCriteriaBuilder) cb;
                var minutes = hibernate.durationByUnit(TemporalUnit.MINUTE,
                        hibernate.durationBetween(root.<LocalDateTime>get("endTime"), root.<LocalDateTime>get("startTime")));
                if (minDuration != null) predicates.add(cb.ge(minutes, minDuration));
                if (maxDuration != null) predicates.add(cb.le(minutes, maxDuration));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static String contains(String search) {
        return "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
