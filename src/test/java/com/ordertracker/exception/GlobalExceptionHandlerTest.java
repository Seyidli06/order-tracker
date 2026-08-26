package com.ordertracker.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MethodParameter methodParameter;

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/orders/10");
    }

    @Test
    void shouldReturnNotFoundForEntityNotFoundException() {
        EntityNotFoundException exception =
                new EntityNotFoundException(
                        "Order not found: 10"
                );

        ResponseEntity<ApiError> response =
                handler.handleEntityNotFound(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals(
                "Order not found: 10",
                body.message()
        );
        assertEquals(
                "/api/orders/10",
                body.path()
        );
        assertEquals(
                Map.of(),
                body.validationErrors()
        );
    }

    @Test
    void shouldReturnForbiddenForAccessDeniedException() {
        AccessDeniedException exception =
                new AccessDeniedException(
                        "You are not allowed to access this order"
                );

        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(403, body.status());
        assertEquals("Forbidden", body.error());
        assertEquals(
                "You are not allowed to access this order",
                body.message()
        );
        assertEquals(
                "/api/orders/10",
                body.path()
        );
    }

    @Test
    void shouldReturnConflictForResourceConflictException() {
        ResourceConflictException exception =
                new ResourceConflictException(
                        "User with this email already exists"
                );

        ResponseEntity<ApiError> response =
                handler.handleResourceConflict(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(409, body.status());
        assertEquals("Conflict", body.error());
        assertEquals(
                "User with this email already exists",
                body.message()
        );
    }

    @Test
    void shouldReturnBadRequestForIllegalArgumentException() {
        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid request"
                );

        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals(
                "Invalid request",
                body.message()
        );
    }

    @Test
    void shouldReturnValidationErrors() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "createOrderRequest"
                );

        bindingResult.addError(
                new FieldError(
                        "createOrderRequest",
                        "currency",
                        "size must be between 3 and 3"
                )
        );

        bindingResult.addError(
                new FieldError(
                        "createOrderRequest",
                        "totalAmount",
                        "must be greater than or equal to 0.01"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        methodParameter,
                        bindingResult
                );

        ResponseEntity<ApiError> response =
                handler.handleValidation(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals(
                "Request validation failed",
                body.message()
        );

        assertEquals(
                "size must be between 3 and 3",
                body.validationErrors().get("currency")
        );

        assertEquals(
                "must be greater than or equal to 0.01",
                body.validationErrors().get("totalAmount")
        );

        assertEquals(
                2,
                body.validationErrors().size()
        );
    }

    @Test
    void shouldReturnUnauthorizedForAuthenticationException() {
        request.setRequestURI("/api/auth/login");

        BadCredentialsException exception =
                new BadCredentialsException(
                        "Bad credentials"
                );

        ResponseEntity<ApiError> response =
                handler.handleAuthentication(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(401, body.status());
        assertEquals(
                "Unauthorized",
                body.error()
        );
        assertEquals(
                "Invalid email or password",
                body.message()
        );
        assertEquals(
                "/api/auth/login",
                body.path()
        );

        assertNotEquals(
                exception.getMessage(),
                body.message()
        );
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedException() {
        RuntimeException exception =
                new RuntimeException(
                        "Database exploded"
                );

        ResponseEntity<ApiError> response =
                handler.handleUnexpectedException(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        ApiError body = response.getBody();

        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals(
                "Internal Server Error",
                body.error()
        );
        assertEquals(
                "An unexpected error occurred",
                body.message()
        );

        assertNotEquals(
                "Database exploded",
                body.message()
        );
    }
}