package io.github.dmitriyiliyov.springoutbox.unit.dlq.api;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.api.OutboxDlqControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutboxDlqControllerAdviceUnitTests {

    HttpServletRequest request;

    OutboxDlqControllerAdvice tested;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tested = new OutboxDlqControllerAdvice();
        request = new MockHttpServletRequest("GET", "/test/uri");
    }

    @Test
    @DisplayName("UT handleValidationExceptions() with MethodArgumentNotValidException, should return ProblemDetail with field errors")
    void handleValidationExceptions_whenMethodArgumentNotValid_shouldReturnProblemDetail() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("obj", "field1", "must not be null");
        FieldError fieldError2 = new FieldError("obj", "field2", "must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ProblemDetail> response = tested.handleValidationExceptions(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail pd = response.getBody();
        assertNotNull(pd);
        assertEquals("/errors/outbox/validation", pd.getType().toString());
        assertEquals("Validation Failed", pd.getTitle());
        assertEquals("Request validation failed", pd.getDetail());
        assertEquals("/test/uri", pd.getInstance().toString());
        assertEquals("/test/uri", pd.getProperties().get("path"));

        List<Map<String, String>> errors = (List<Map<String, String>>) pd.getProperties().get("errors");
        assertEquals(2, errors.size());
        assertEquals("field1", errors.get(0).get("field"));
        assertEquals("must not be null", errors.get(0).get("message"));
        assertEquals("field2", errors.get(1).get("field"));
        assertEquals("must be positive", errors.get(1).get("message"));
    }

    @Test
    @DisplayName("UT handleValidationExceptions() with BindException, should return ProblemDetail with field errors")
    void handleValidationExceptions_whenBindException_shouldReturnProblemDetail() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "name", "cannot be empty");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        BindException ex = mock(BindException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ProblemDetail> response = tested.handleValidationExceptions(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail pd = response.getBody();
        assertNotNull(pd);

        List<Map<String, String>> errors = (List<Map<String, String>>) pd.getProperties().get("errors");
        assertEquals(1, errors.size());
        assertEquals("name", errors.get(0).get("field"));
        assertEquals("cannot be empty", errors.get(0).get("message"));
    }

    @Test
    @DisplayName("UT handleValidationExceptions() with unknown exception, should return ProblemDetail with empty errors")
    void handleValidationExceptions_whenUnknownException_shouldReturnEmptyErrors() {
        // given
        Exception ex = new Exception("some exception");

        // when
        ResponseEntity<ProblemDetail> response = tested.handleValidationExceptions(ex, request);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail pd = response.getBody();
        assertNotNull(pd);
        List<?> errors = (List<?>) pd.getProperties().get("errors");
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }
}
