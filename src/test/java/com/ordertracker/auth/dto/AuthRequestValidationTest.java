package com.ordertracker.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @Test
    void shouldAcceptValidLoginRequest() {
        LoginRequest request =
                new LoginRequest(
                        "user@test.com",
                        "password123"
                );

        Set<ConstraintViolation<LoginRequest>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectLoginEmailAboveMaximumLength() {
        LoginRequest request =
                new LoginRequest(
                        "a".repeat(250)
                                + "@test.com",
                        "password123"
                );

        Set<ConstraintViolation<LoginRequest>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectLoginPasswordAboveMaximumLength() {
        LoginRequest request =
                new LoginRequest(
                        "user@test.com",
                        "a".repeat(101)
                );

        Set<ConstraintViolation<LoginRequest>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldAcceptValidRegisterRequest() {
        RegisterRequest request =
                new RegisterRequest(
                        "user@test.com",
                        "password123"
                );

        Set<ConstraintViolation<RegisterRequest>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectRegisterPasswordBelowMinimumLength() {
        RegisterRequest request =
                new RegisterRequest(
                        "user@test.com",
                        "1234567"
                );

        Set<ConstraintViolation<RegisterRequest>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectRegisterPasswordAboveMaximumLength() {
        RegisterRequest request =
                new RegisterRequest(
                        "user@test.com",
                        "a".repeat(101)
                );

        Set<ConstraintViolation<RegisterRequest>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectRegisterEmailAboveMaximumLength() {
        RegisterRequest request =
                new RegisterRequest(
                        "a".repeat(250)
                                + "@test.com",
                        "password123"
                );

        Set<ConstraintViolation<RegisterRequest>>
                violations =
                validator.validate(request);

        assertFalse(
                violations.isEmpty()
        );
    }
}