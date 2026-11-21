package ru.mirea.officebooking.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.officebooking.service.BookingNoteService;
import ru.mirea.officebooking.service.BookingNoteService.Note;
import ru.mirea.officebooking.service.BookingNoteService.Request;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BookingNoteController {
    private final BookingNoteService notes;

    public BookingNoteController(BookingNoteService notes) {
        this.notes = notes;
    }

    @GetMapping("/bookings/{id}/notes")
    public List<Note> list(@PathVariable UUID id) {
        return notes.list(id);
    }

    @PostMapping("/bookings/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Note create(@PathVariable UUID id, @Valid @RequestBody Request request) {
        return notes.create(id, request);
    }

    @PutMapping("/notes/{id}")
    public Note update(@PathVariable UUID id, @Valid @RequestBody Request request) {
        return notes.update(id, request);
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        notes.delete(id);
    }
}
