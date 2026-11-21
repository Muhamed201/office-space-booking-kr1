package ru.mirea.officebooking.service;

import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.*;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.filter.Pages;
import ru.mirea.officebooking.repository.AuditEventRepository;
import ru.mirea.officebooking.repository.UserAccountRepository;
import ru.mirea.officebooking.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuditService {
    public record Entry(UUID id, UUID userId, EventType eventType, String entityType, UUID entityId,
                        String description, String correlationId, LocalDateTime createdAt) {}

    private final AuditEventRepository repository;
    private final UserAccountRepository users;

    public AuditService(AuditEventRepository repository, UserAccountRepository users) {
        this.repository = repository;
        this.users = users;
    }

    public void logEvent(EventType type, String entityType, UUID entityId, String description) {
        var actor = CurrentUser.optional();
        record(actor == null ? null : users.getReferenceById(actor.id()), type, entityType, entityId, description);
    }

    public void record(UserAccount actor, EventType type, String entityType, UUID entityId, String description) {
        AuditEvent event = new AuditEvent(type, description);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setCorrelationId(MDC.get("correlationId"));
        event.setUser(actor);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<Entry> list(String search, EventType type, Pageable pageable) {
        Specification<AuditEvent> specification = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = BookingFilter.contains(search);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("description")), pattern, '\\'),
                    cb.like(cb.lower(root.get("entityType")), pattern, '\\'),
                    cb.like(cb.lower(root.get("correlationId")), pattern, '\\')));
        }
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("eventType"), type));
        }
        return repository.findAll(specification, Pages.sorted(pageable, Set.of("id", "createdAt", "eventType", "entityType"),
                Sort.by(Sort.Direction.DESC, "createdAt"))).map(event -> new Entry(event.getId(),
                event.getUser() == null ? null : event.getUser().getId(), event.getEventType(), event.getEntityType(),
                event.getEntityId(), event.getDescription(), event.getCorrelationId(), event.getCreatedAt()));
    }
}
