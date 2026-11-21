package ru.mirea.officebooking.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.officebooking.dto.BulkResult;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.service.CsvService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/bookings")
public class CsvController {
    private final CsvService csv;

    public CsvController(CsvService csv) {
        this.csv = csv;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@Valid @ModelAttribute BookingFilter filter, Sort sort) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookings.csv")
                .body(csv.export(filter, sort));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkResult preview(@RequestParam MultipartFile file, @RequestParam CsvService.Mode mode) {
        return csv.read(file, mode, true);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkResult apply(@RequestParam MultipartFile file, @RequestParam CsvService.Mode mode) {
        return csv.read(file, mode, false);
    }
}
