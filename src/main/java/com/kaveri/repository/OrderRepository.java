package com.kaveri.repository;

import com.kaveri.entity.Order;
import com.kaveri.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:keyword IS NULL OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%') " +
           "OR LOWER(o.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR o.orderStatus = :status)")
    Page<Order> searchOrders(
        @Param("keyword") String keyword,
        @Param("status") OrderStatus status,
        Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    long countTodaysOrders(@Param("startOfDay") LocalDateTime startOfDay,
                           @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.paymentStatus = com.kaveri.enums.PaymentStatus.PAID " +
           "OR o.paymentStatus = com.kaveri.enums.PaymentStatus.COD")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") OrderStatus status);
}
