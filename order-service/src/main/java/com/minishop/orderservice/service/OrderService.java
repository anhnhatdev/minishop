package com.minishop.orderservice.service;

import com.minishop.orderservice.dto.request.CancelOrderRequest;
import com.minishop.orderservice.dto.request.CheckoutRequest;
import com.minishop.orderservice.dto.request.UpdateOrderStatusRequest;
import com.minishop.orderservice.dto.response.OrderDetailResponse;
import com.minishop.orderservice.dto.response.OrderResponse;
import com.minishop.orderservice.dto.response.PagedResponse;
import com.minishop.orderservice.entity.*;
import com.minishop.orderservice.event.dto.OrderCancelledEvent;
import com.minishop.orderservice.event.dto.OrderCreatedEvent;
import com.minishop.orderservice.event.producer.OrderEventProducer;
import com.minishop.orderservice.exception.InvalidOrderStatusTransitionException;
import com.minishop.orderservice.exception.OrderNotFoundException;
import com.minishop.orderservice.mapper.OrderMapper;
import com.minishop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Random random = new Random();

    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request) {
        Cart cart = cartService.getOrCreateCartEntity(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart");
        }

        String orderCode = generateOrderCode();
        BigDecimal totalAmount = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .shippingPhone(request.getShippingPhone())
                .paymentMethod(request.getPaymentMethod())
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        List<OrderCreatedEvent.OrderItemEventDto> eventItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            order.addItem(orderItem);

            eventItems.add(OrderCreatedEvent.OrderItemEventDto.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());
        }

        order.addStatusHistory(null, OrderStatus.PENDING, "Order created, waiting for inventory stock reservation");
        Order savedOrder = orderRepository.save(order);

        // Clear user cart after creating order
        cartService.clearCart(userId);

        // Start Saga Orchestration by publishing OrderCreatedEvent
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("order.created")
                .orderId(savedOrder.getId())
                .orderCode(savedOrder.getOrderCode())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .paymentMethod(savedOrder.getPaymentMethod().name())
                .items(eventItems)
                .timestamp(Instant.now())
                .build();

        orderEventProducer.publishOrderCreated(event);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> orderPage = (status != null)
                ? orderRepository.findByUserIdAndStatus(userId, status, pageable)
                : orderRepository.findByUserId(userId, pageable);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(orderMapper::toOrderResponse)
                .toList();

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID orderId, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to view this order");
        }

        return orderMapper.toOrderDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse cancelOrder(UUID orderId, UUID userId, boolean isAdmin, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to cancel this order");
        }

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot cancel order in state: " + order.getStatus() + " (cancellation only allowed prior to shipping)");
        }

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Cancelled by user";
        order.addStatusHistory(fromStatus, OrderStatus.CANCELLED, reason);
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCancelledEvent to trigger compensation (release reserved inventory if needed)
        OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("order.cancelled")
                .orderId(savedOrder.getId())
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        orderEventProducer.publishOrderCancelled(cancelEvent);

        return orderMapper.toOrderDetailResponse(savedOrder);
    }

    @Transactional
    public OrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        OrderStatus newStatus = request.getStatus();
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition order status from " + order.getStatus() + " to " + newStatus);
        }

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(newStatus);
        String note = request.getNote() != null ? request.getNote() : "Status updated to " + newStatus;
        order.addStatusHistory(fromStatus, newStatus, note);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderDetailResponse(savedOrder);
    }

    private String generateOrderCode() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        try {
            Long seq = orderRepository.getNextOrderSequence();
            if (seq != null) {
                return String.format("ORD%s%06d", datePart, seq % 1000000);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch order sequence from DB, falling back to secure random: {}", e.getMessage());
        }
        int randomPart = 100000 + random.nextInt(900000);
        return "ORD" + datePart + randomPart;
    }
}
