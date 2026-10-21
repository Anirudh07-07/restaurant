package com.kaveri.util;

import com.kaveri.enums.OrderStatus;
import com.kaveri.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PLACED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BadRequestException(String.format(
                    "Invalid order status transition from '%s' to '%s'. Allowed transitions: %s",
                    currentStatus, newStatus, allowed.isEmpty() ? "None (Terminal status)" : allowed
            ));
        }
    }

    public boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus);
    }
}

