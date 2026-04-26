package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.BadRequestException;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.NotFoundException;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.UnknownDlqStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = OutboxDlqController.class)
public class OutboxDlqControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqControllerAdvice.class);

    private final Clock clock;

    public OutboxDlqControllerAdvice(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", request.getRequestURI(), e);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/errors/outbox/unexpected",
                "Internal Server Error",
                e.getMessage(),
                request.getRequestURI(),
                clock.instant()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "/errors/outbox/not-found",
                "Not Found",
                e.getDetail(),
                request.getRequestURI(),
                clock.instant()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleNotFoundException(BadRequestException e, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/bad-request",
                "Bad Request",
                e.getDetail(),
                request.getRequestURI(),
                clock.instant()
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleValidationExceptions(Exception e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/validation",
                "Validation Failed",
                "Request validation failed",
                request.getRequestURI(),
                clock.instant()
        );

        List<Map<String, String>> errors = new ArrayList<>();

        if (e instanceof MethodArgumentNotValidException ex) {
            ex.getBindingResult().getFieldErrors().forEach(error ->
                    errors.add(Map.of("field", error.getField(), "message", error.getDefaultMessage()))
            );
        } else if (e instanceof BindException ex) {
            ex.getBindingResult().getFieldErrors().forEach(error ->
                    errors.add(Map.of("field", error.getField(), "message", error.getDefaultMessage()))
            );
        } else {
            Throwable cause = e.getCause();
            while (true) {
                Throwable newCause = cause.getCause();
                if (newCause == null) {
                    break;
                }
                cause = newCause;
            }
            if (cause instanceof UnknownDlqStatusException udse) {
                problemDetail.setDetail(udse.getDetail());
            } else {
                errors.add(Map.of("message", e.getMessage()));
            }
        }
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/validation/constraint-violation",
                "Validation Failed",
                "Constraint validation failed",
                request.getRequestURI(),
                clock.instant()
        );
        List<Map<String, String>> violations = e.getConstraintViolations().stream()
                .map(v -> Map.of("property", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();

        problemDetail.setProperty("errors", violations);
        return problemDetail;
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDatabaseError(DataAccessException e, HttpServletRequest request) {
        log.error("DLQ database error: {}", request.getRequestURI(), e);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/errors/outbox/database",
                "Database Access Error",
                "A database operation failed: " + e.getMostSpecificCause().getMessage(),
                request.getRequestURI(),
                clock.instant()
        );
    }

    private ProblemDetail createProblemDetail(HttpStatus httpStatus,
                                              String type,
                                              String title,
                                              String detail,
                                              String path,
                                              Instant timestamp) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("path", path);
        problemDetail.setProperty("timestamp", timestamp);
        return problemDetail;
    }
}
