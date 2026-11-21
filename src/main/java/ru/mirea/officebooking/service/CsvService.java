package ru.mirea.officebooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.apache.commons.csv.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.officebooking.domain.BookingStatus;
import ru.mirea.officebooking.dto.*;
import ru.mirea.officebooking.error.*;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.repository.BookingRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvService {
    public enum Mode { CREATE, UPDATE }

    private static final String[] COLUMNS = {"bookingNumber", "employeeId", "workspaceId", "startTime", "endTime",
            "status", "attendees", "purpose", "description", "isRecurring", "requiresEquipment", "tags"};
    private static final CSVFormat INPUT = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();

    private final BookingService bookings;
    private final BookingRepository repository;
    private final TransactionTemplate transaction;
    private final Validator validator;
    private final ObjectMapper mapper;

    public CsvService(BookingService bookings, BookingRepository repository, TransactionTemplate transaction,
                      Validator validator, ObjectMapper mapper) {
        this.bookings = bookings;
        this.repository = repository;
        this.transaction = transaction;
        this.validator = validator;
        this.mapper = mapper;
    }

    public String export(BookingFilter filter, Sort sort) {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(COLUMNS).get())) {
            for (BookingDto booking : bookings.exportBookings(filter, sort)) {
                printer.printRecord(booking.getBookingNumber(), booking.getEmployeeId(), booking.getWorkspaceId(),
                        booking.getStartTime(), booking.getEndTime(), booking.getStatus(), booking.getAttendees(),
                        safeText(booking.getPurpose()), safeText(booking.getDescription()), booking.getIsRecurring(),
                        booking.getRequiresEquipment(), mapper.writeValueAsString(booking.getTags()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }

    public BulkResult read(MultipartFile file, Mode mode, boolean preview) {
        bookings.requireManager();
        var errors = new ArrayList<String>();
        var applied = new ArrayList<BookingDto>();
        var seen = new HashSet<String>();
        int success = 0;
        try (var reader = new PushbackReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), 1)) {
            int first = reader.read();
            if (first != -1 && first != 0xfeff) reader.unread(first);
            try (CSVParser parser = INPUT.parse(reader)) {
                if (parser.getHeaderNames().size() != COLUMNS.length ||
                        !new HashSet<>(parser.getHeaderNames()).equals(Set.of(COLUMNS))) {
                    throw new ValidationException("Неверные заголовки CSV. Используйте колонки экспорта");
                }
                // Файл проверяется целиком до записи первой строки.
                for (CSVRecord row : parser.getRecords()) {
                    try {
                        BookingDto booking = transaction.execute(status -> {
                            BookingDto result = process(row, mode, preview, seen);
                            if (preview) status.setRollbackOnly();
                            return result;
                        });
                        if (preview) {
                            checkOtherRows(booking, applied);
                            applied.add(booking);
                        }
                        if (mode == Mode.UPDATE) seen.add(row.get("bookingNumber"));
                        success++;
                    } catch (ValidationException | EntityNotFoundException | ConflictException | IllegalArgumentException |
                             DateTimeParseException e) {
                        errors.add("Строка " + row.getRecordNumber() + ": " + e.getMessage());
                    } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                        errors.add("Строка " + row.getRecordNumber() + ": конфликт с существующей записью");
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ValidationException("Не удалось прочитать CSV. Проверьте кодировку UTF-8 и формат файла");
        }
        return new BulkResult(success, errors.size(), errors);
    }

    private BookingDto process(CSVRecord row, Mode mode, boolean preview, Set<String> seen) {
        if (!row.isConsistent()) {
            throw new ValidationException("Количество полей отличается от заголовка");
        }
        String number = row.get("bookingNumber");
        String status = row.get("status");
        var data = new CreateBookingRequest();
        data.setEmployeeId(parseId(row.get("employeeId"), "employeeId"));
        data.setWorkspaceId(parseId(row.get("workspaceId"), "workspaceId"));
        data.setStartTime(LocalDateTime.parse(row.get("startTime")));
        data.setEndTime(LocalDateTime.parse(row.get("endTime")));
        var attendees = row.get("attendees");
        data.setAttendees(attendees.isBlank() ? null : Integer.valueOf(attendees));
        data.setPurpose(readText(row.get("purpose")));
        data.setDescription(readText(row.get("description")));
        data.setIsRecurring(parseFlag(row.get("isRecurring")));
        data.setRequiresEquipment(parseFlag(row.get("requiresEquipment")));
        data.setTags(parseTags(row.get("tags")));
        validate(data);

        if (mode == Mode.CREATE) {
            if (!status.isBlank() && !status.equals(BookingStatus.ACTIVE.name())) {
                throw new ValidationException("Новая бронь должна иметь статус ACTIVE");
            }
            return bookings.createBooking(data, preview ? "TMP-" + UUID.randomUUID().toString().substring(0, 16) : null);
        }

        if (seen.contains(number)) {
            throw new ValidationException("Код брони повторяется в файле");
        }
        var existing = repository.findByBookingNumber(number)
                .orElseThrow(() -> new EntityNotFoundException("Бронь с таким номером не найдена"));
        if (!existing.getEmployee().getId().equals(data.getEmployeeId())) {
            throw new ValidationException("Сотрудника существующей брони менять нельзя");
        }
        var update = new UpdateBookingRequest();
        update.setVersion(existing.getVersion());
        update.setStatus(BookingStatus.valueOf(status));
        update.setWorkspaceId(data.getWorkspaceId());
        update.setStartTime(data.getStartTime());
        update.setEndTime(data.getEndTime());
        update.setAttendees(data.getAttendees());
        update.setPurpose(data.getPurpose());
        update.setDescription(data.getDescription());
        update.setIsRecurring(data.getIsRecurring());
        update.setRequiresEquipment(data.getRequiresEquipment());
        update.setTags(data.getTags());
        validate(update);
        return bookings.updateBooking(existing.getId(), update);
    }

    private void checkOtherRows(BookingDto booking, List<BookingDto> otherRows) {
        if (booking.getStatus() != BookingStatus.ACTIVE) return;
        boolean overlaps = otherRows.stream().anyMatch(other -> other.getStatus() == BookingStatus.ACTIVE
                && (other.getEmployeeId().equals(booking.getEmployeeId()) || other.getWorkspaceId().equals(booking.getWorkspaceId()))
                && other.getStartTime().isBefore(booking.getEndTime()) && other.getEndTime().isAfter(booking.getStartTime()));
        if (overlaps) throw new ConflictException("Интервал пересекается с другой строкой CSV");
    }

    private void validate(Object data) {
        var errors = validator.validate(data);
        if (!errors.isEmpty()) {
            var error = errors.iterator().next();
            throw new ValidationException(error.getPropertyPath() + ": " + error.getMessage());
        }
    }

    private UUID parseId(String value, String column) {
        if (!value.matches("(?i)[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
            throw new ValidationException("Неверный UUID в колонке " + column);
        }
        return UUID.fromString(value);
    }

    private Boolean parseFlag(String value) {
        return switch (value) {
            case "true" -> true;
            case "", "false" -> false;
            default -> throw new ValidationException("Логическое значение должно быть true или false");
        };
    }

    private List<String> parseTags(String value) {
        if (value.isBlank()) return List.of();
        try {
            String[] tags = mapper.readValue(value, String[].class);
            if (tags == null) throw new ValidationException("Теги должны быть JSON-массивом строк");
            return Arrays.asList(tags);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Теги должны быть JSON-массивом строк");
        }
    }

    private String safeText(String value) {
        return value != null && needsPrefix(value) ? "'" + value : value;
    }

    private String readText(String value) {
        if (value.isEmpty()) return null;
        return value.charAt(0) == '\'' && needsPrefix(value.substring(1)) ? value.substring(1) : value;
    }

    private boolean needsPrefix(String value) {
        return !value.isEmpty() && ("=+@-'".indexOf(value.charAt(0)) >= 0 || Character.isWhitespace(value.charAt(0)));
    }
}
