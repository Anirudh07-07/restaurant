package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.PlaceOrderRequest;
import com.foodnest.foodnest.dto.response.OrderResponse;
import com.foodnest.foodnest.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    Page<OrderResponse> getUserOrders(int page, int size);

    OrderResponse getOrderById(Long id);

    OrderResponse cancelOrder(Long id);

    Page<OrderResponse> getAllOrders(String keyword, OrderStatus status, int page, int size);

    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);
}
