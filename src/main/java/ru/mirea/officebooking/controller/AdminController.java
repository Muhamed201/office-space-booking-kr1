package ru.mirea.officebooking.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.officebooking.domain.EventType;
import ru.mirea.officebooking.service.AdminUserService;
import ru.mirea.officebooking.service.AuditService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminUserService users;
    private final AuditService audit;

    public AdminController(AdminUserService users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping("/users")
    public PagedModel<AdminUserService.Account> users(Pageable pageable) {
        return new PagedModel<>(users.list(pageable));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserService.Account create(@Valid @RequestBody AdminUserService.Create input) {
        return users.create(input);
    }

    @PutMapping("/users/{id}")
    public AdminUserService.Account update(@PathVariable UUID id, @Valid @RequestBody AdminUserService.Update input) {
        return users.update(id, input);
    }

    @PostMapping("/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable UUID id, @Valid @RequestBody AdminUserService.Reset input) {
        users.reset(id, input);
    }

    @GetMapping("/audit")
    public PagedModel<AuditService.Entry> audit(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) EventType type, Pageable pageable) {
        return new PagedModel<>(audit.list(q, type, pageable));
    }
}
