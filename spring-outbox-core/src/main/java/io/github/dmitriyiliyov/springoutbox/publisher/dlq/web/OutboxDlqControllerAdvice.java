package io.github.dmitriyiliyov.springoutbox.publisher.dlq.web;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.BadRequestException;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = {OutboxDlqController.class})
public class OutboxDlqControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", request.getRequestURI(), e);
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/errors/outbox/unexpected",
                "Internal Server Error",
                e.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "/errors/outbox/not-found",
                "Not Found",
                e.getDetail(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(BadRequestException e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/bad-request",
                "Bad Request",
                e.getDetail(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ProblemDetail> handleValidationExceptions(Exception e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/validation",
                "Validation Failed",
                "Request validation failed",
                request.getRequestURI(),
                Instant.now()
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
        }
        problemDetail.setProperty("errors", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "/errors/outbox/validation/constraint-violation",
                "Validation Failed",
                "Constraint validation failed",
                request.getRequestURI(),
                Instant.now()
        );
        List<Map<String, String>> violations = e.getConstraintViolations().stream()
                .map(v -> Map.of("property", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();

        problemDetail.setProperty("errors", violations);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDatabaseError(DataAccessException e, HttpServletRequest request) {
        log.error("DLQ database error: {}", request.getRequestURI(), e);
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "/errors/outbox/database",
                "Database Access Error",
                "A database operation failed: " + e.getMostSpecificCause().getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
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
