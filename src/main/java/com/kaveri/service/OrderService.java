package com.kaveri.service;

import com.kaveri.dto.request.PlaceOrderRequest;
import com.kaveri.dto.response.OrderResponse;
import com.kaveri.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    Page<OrderResponse> getUserOrders(int page, int size);

    OrderResponse getOrderById(Long id);

    OrderResponse cancelOrder(Long id);

    Page<OrderResponse> getAllOrders(String keyword, OrderStatus status, int page, int size);

    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);
}
