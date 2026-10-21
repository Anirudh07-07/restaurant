package com.kaveri.util;

import com.kaveri.enums.OrderStatus;
import com.kaveri.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTransitionValidatorTest {

    private OrderStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderStatusTransitionValidator();
    }

    @Test
    @DisplayName("Valid forward order lifecycle transitions should pass")
    void validForwardTransitions_pass() {
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PLACED, OrderStatus.CONFIRMED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.CONFIRMED, OrderStatus.PREPARING));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PREPARING, OrderStatus.READY));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.READY, OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Cancellation from non-terminal states should pass")
    void cancellationFromActiveStates_pass() {
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PLACED, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.READY, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("Transition to identical status is an allowed no-op")
    void identicalStatus_pass() {
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PREPARING, OrderStatus.PREPARING));
    }

    @Test
    @DisplayName("Invalid backwards or jumping transitions should throw BadRequestException")
    void invalidTransitions_throwBadRequest() {
        // Jumping from PLACED directly to DELIVERED
        assertThrows(BadRequestException.class, () ->
                validator.validateTransition(OrderStatus.PLACED, OrderStatus.DELIVERED));

        // Moving backward from DELIVERED to PLACED
        assertThrows(BadRequestException.class, () ->
                validator.validateTransition(OrderStatus.DELIVERED, OrderStatus.PLACED));

        // Reopening CANCELLED order
        assertThrows(BadRequestException.class, () ->
                validator.validateTransition(OrderStatus.CANCELLED, OrderStatus.PREPARING));

        // Moving backward from OUT_FOR_DELIVERY to PREPARING
        assertThrows(BadRequestException.class, () ->
                validator.validateTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.PREPARING));
    }
}

