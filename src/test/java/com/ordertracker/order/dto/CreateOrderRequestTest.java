package com.ordertracker.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidOrderRequest() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        new BigDecimal("149.99"),
                        "AZN"
                );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }

    @Test
    void shouldRejectAmountBelowMinimum() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        BigDecimal.ZERO,
                        "AZN"
                );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations =
                validator.validate(request);

        assertEquals(
                1,
                violations.size()
        );

        assertEquals(
                "Total amount must be at least 0.01",
                violations.iterator()
                        .next()
                        .getMessage()
        );
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        new BigDecimal("100.00"),
                        "1$!"
                );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations =
                validator.validate(request);

        assertEquals(
                1,
                violations.size()
        );

        assertEquals(
                "Currency must be a 3-letter code",
                violations.iterator()
                        .next()
                        .getMessage()
        );
    }

    @Test
    void shouldAcceptLowercaseCurrencyCode() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        new BigDecimal("100.00"),
                        "azn"
                );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations =
                validator.validate(request);

        assertTrue(
                violations.isEmpty()
        );
    }
}