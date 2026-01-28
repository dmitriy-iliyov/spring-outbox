package io.github.dmitriyiliyov.springoutbox.unit.publisher.dlq.web;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.OutboxDlqControllerAdvice;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.BadRequestException;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDlqControllerAdviceUnitTests {

    @Mock
    HttpServletRequest request;

    OutboxDlqControllerAdvice tested;

    @BeforeEach
    void setUp() {
        tested = new OutboxDlqControllerAdvice();
        when(request.getRequestURI()).thenReturn("/test/uri");
    }

    @Test
    @DisplayName("UT handleException() should return 500 and ProblemDetail")
    void handleException_shouldReturn500() {
        // given
        Exception ex = new Exception("Unexpected error");

        // when
        ResponseEntity<ProblemDetail> response = tested.handleException(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/unexpected"), body.getType());
        assertEquals("Internal Server Error", body.getTitle());
        assertEquals("Unexpected error", body.getDetail());
        assertEquals("/test/uri", body.getProperties().get("path"));
    }

    @Test
    @DisplayName("UT handleNotFoundException() should return 404 and ProblemDetail")
    void handleNotFoundException_shouldReturn404() {
        // given
        NotFoundException ex = mock(NotFoundException.class);
        when(ex.getDetail()).thenReturn("Resource not found");

        // when
        ResponseEntity<ProblemDetail> response = tested.handleNotFoundException(ex, request);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/not-found"), body.getType());
        assertEquals("Not Found", body.getTitle());
        assertEquals("Resource not found", body.getDetail());
    }

    @Test
    @DisplayName("UT handleBadRequestException() should return 400 and ProblemDetail")
    void handleBadRequestException_shouldReturn400() {
        // given
        BadRequestException ex = mock(BadRequestException.class);
        when(ex.getDetail()).thenReturn("Bad request detail");

        // when
        ResponseEntity<ProblemDetail> response = tested.handleNotFoundException(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/bad-request"), body.getType());
        assertEquals("Bad Request", body.getTitle());
        assertEquals("Bad request detail", body.getDetail());
    }

    @Test
    @DisplayName("UT handleValidationExceptions() with MethodArgumentNotValidException should return 400 and errors")
    void handleValidationExceptions_withMethodArgumentNotValid_shouldReturn400() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "defaultMessage");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // when
        ResponseEntity<ProblemDetail> response = tested.handleValidationExceptions(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/validation"), body.getType());
        List<Map<String, String>> errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertEquals(1, errors.size());
        assertEquals("field", errors.get(0).get("field"));
        assertEquals("defaultMessage", errors.get(0).get("message"));
    }

    @Test
    @DisplayName("UT handleValidationExceptions() with BindException should return 400 and errors")
    void handleValidationExceptions_withBindException_shouldReturn400() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "defaultMessage");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        BindException ex = new BindException(bindingResult);

        // when
        ResponseEntity<ProblemDetail> response = tested.handleValidationExceptions(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/validation"), body.getType());
        List<Map<String, String>> errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertEquals(1, errors.size());
        assertEquals("field", errors.get(0).get("field"));
        assertEquals("defaultMessage", errors.get(0).get("message"));
    }

    @Test
    @DisplayName("UT handleConstraintViolation() should return 400 and errors")
    void handleConstraintViolation_shouldReturn400() {
        // given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("prop");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("violation message");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<ProblemDetail> response = tested.handleConstraintViolation(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/validation/constraint-violation"), body.getType());
        List<Map<String, String>> errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertEquals(1, errors.size());
        assertEquals("prop", errors.get(0).get("property"));
        assertEquals("violation message", errors.get(0).get("message"));
    }

    @Test
    @DisplayName("UT handleDatabaseError() should return 500 and ProblemDetail")
    void handleDatabaseError_shouldReturn500() {
        // given
        DataAccessException ex = mock(DataAccessException.class);
        Throwable cause = new RuntimeException("Connection refused");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        // when
        ResponseEntity<ProblemDetail> response = tested.handleDatabaseError(ex, request);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(URI.create("/errors/outbox/database"), body.getType());
        assertEquals("Database Access Error", body.getTitle());
        assertEquals("A database operation failed: Connection refused", body.getDetail());
    }
}
