package ru.mirea.officebooking.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.mirea.officebooking.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({EntityNotFoundException.class, ValidationException.class, AuthenticationException.class,
            AccessDeniedException.class, ConflictException.class, OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> applicationError(RuntimeException exception, HttpServletRequest request) {
        return switch (exception) {
            case EntityNotFoundException e -> error(HttpStatus.NOT_FOUND, e.getMessage(), request);
            case ValidationException e -> error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
            case AuthenticationException e -> error(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
            case AccessDeniedException e -> error(HttpStatus.FORBIDDEN, e.getMessage(), request);
            case ConflictException e -> error(HttpStatus.CONFLICT, e.getMessage(), request);
            case OptimisticLockingFailureException e ->
                    error(HttpStatus.CONFLICT, "Запись изменилась. Обновите данные", request);
            case DataIntegrityViolationException e -> {
                var cause = e.getMostSpecificCause();
                String message = cause instanceof java.sql.SQLException sql && "23P01".equals(sql.getSQLState())
                        ? "Место или сотрудник уже заняты на выбранное время"
                        : "Данные конфликтуют с существующей записью";
                yield error(HttpStatus.CONFLICT, message, request);
            }
            default -> throw exception;
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .distinct().collect(java.util.stream.Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isBlank() ? "Проверьте поля запроса" : message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> invalidRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Проверьте формат и обязательные поля запроса", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> uploadSize(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "CSV-файл слишком большой", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> otherError(Exception exception, HttpServletRequest request) {
        if (exception instanceof org.springframework.web.ErrorResponse standard) {
            var status = standard.getStatusCode();
            var known = HttpStatus.resolve(status.value());
            String title = known == null ? "Ошибка запроса" : known.getReasonPhrase();
            return ResponseEntity.status(status).headers(standard.getHeaders()).body(new ErrorResponse(
                    status.value(), title, "Запрос не удалось обработать", request.getRequestURI()));
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Необработанная ошибка API", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось обработать запрос", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
