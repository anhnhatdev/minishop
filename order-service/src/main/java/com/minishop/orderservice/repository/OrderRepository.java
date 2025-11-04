package com.minishop.orderservice.repository;

import com.minishop.orderservice.entity.Order;
import com.minishop.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant timestamp);

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('order_code_seq')", nativeQuery = true)
    Long getNextOrderSequence();
}
