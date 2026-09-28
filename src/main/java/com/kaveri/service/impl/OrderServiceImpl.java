package com.kaveri.service.impl;

import com.kaveri.dto.request.PlaceOrderRequest;
import com.kaveri.dto.response.OrderResponse;
import com.kaveri.entity.*;
import com.kaveri.enums.OrderStatus;
import com.kaveri.enums.PaymentMethod;
import com.kaveri.enums.PaymentStatus;
import com.kaveri.exception.BadRequestException;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.*;
import com.kaveri.service.NotificationService;
import com.kaveri.service.OrderService;
import com.kaveri.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final FoodItemRepository foodItemRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Value("${app.tax.rate:0.05}")
    private BigDecimal taxRate;

    @Value("${app.delivery.fee:40.00}")
    private BigDecimal deliveryFee;

    @Value("${app.delivery.free-above:500.00}")
    private BigDecimal freeDeliveryAbove;

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = securityUtils.getCurrentUser();

        // 1. Validate cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty. Add items before placing an order.");
        }

        // 2. Validate delivery address belongs to user
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This address does not belong to you");
        }

        // 3. Check food availability and fetch current prices
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            FoodItem food = foodItemRepository.findById(cartItem.getFoodItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id",
                            cartItem.getFoodItem().getId()));

            if (!food.isAvailable()) {
                throw new BadRequestException("'" + food.getName() + "' is no longer available");
            }

            // Price snapshot — uses current DB price, not cart price
            BigDecimal currentPrice = food.getPrice();
            BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            return OrderItem.builder()
                    .foodName(food.getName())
                    .foodPrice(currentPrice)
                    .foodImageUrl(food.getImageUrl())
                    .quantity(cartItem.getQuantity())
                    .subtotal(itemSubtotal)
                    .build();
        }).collect(Collectors.toList());

        // 4. Calculate totals
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal appliedDeliveryFee = subtotal.compareTo(freeDeliveryAbove) >= 0
                ? BigDecimal.ZERO : deliveryFee;
        BigDecimal total = subtotal.add(tax).add(appliedDeliveryFee);

        // 5. Create order
        Order order = Order.builder()
                .user(user)
                .deliveryAddress(address.getHouseNumber() + ", " + address.getStreet())
                .deliveryCity(address.getCity())
                .deliveryPincode(address.getPincode())
                .deliveryPhone(address.getPhoneNumber())
                .subtotal(subtotal)
                .tax(tax)
                .deliveryFee(appliedDeliveryFee)
                .totalAmount(total)
                .orderStatus(OrderStatus.PLACED)
                .paymentStatus(request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                        ? PaymentStatus.COD : PaymentStatus.PENDING)
                .build();

        // 6. Link order items to order
        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 7. Create payment record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .amount(total)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                        ? PaymentStatus.COD : PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // 8. Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        // 9. Notify customer
        notificationService.createNotification(user,
                "Order Placed! 🎉",
                "Your order #" + savedOrder.getId() + " has been placed successfully. " +
                "Total: ₹" + total);

        log.info("Order #{} placed by user {}", savedOrder.getId(), user.getEmail());
        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(int page, int size) {
        User user = securityUtils.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        User user = securityUtils.getCurrentUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        User user = securityUtils.getCurrentUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (!canBeCancelled(order.getOrderStatus())) {
            throw new BadRequestException("Order cannot be cancelled at status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Refund if already paid online
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        });

        Order saved = orderRepository.save(order);
        notificationService.createNotification(user, "Order Cancelled",
                "Order #" + id + " has been cancelled.");

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String keyword, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.searchOrders(keyword, status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);

        notificationService.createNotification(order.getUser(),
                "Order Update",
                "Your order #" + id + " is now: " + newStatus.name().replace("_", " "));

        return toResponse(saved);
    }

    private boolean canBeCancelled(OrderStatus status) {
        return status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .foodName(item.getFoodName())
                        .foodPrice(item.getFoodPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .foodImageUrl(item.getFoodImageUrl())
                        .build())
                .collect(Collectors.toList());

        // Get payment method from payment record
        PaymentMethod method = paymentRepository.findByOrderId(order.getId())
                .map(Payment::getPaymentMethod)
                .orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryCity(order.getDeliveryCity())
                .deliveryPincode(order.getDeliveryPincode())
                .deliveryPhone(order.getDeliveryPhone())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(method)
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
