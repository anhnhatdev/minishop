package com.minishop.orderservice.event.consumer;

import com.minishop.orderservice.entity.Order;
import com.minishop.orderservice.entity.OrderStatus;
import com.minishop.orderservice.entity.ProcessedEvent;
import com.minishop.orderservice.event.dto.*;
import com.minishop.orderservice.event.producer.OrderEventProducer;
import com.minishop.orderservice.repository.OrderRepository;
import com.minishop.orderservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderEventProducer orderEventProducer;

    @KafkaListener(topics = "stock.reserved", groupId = "order-service-group")
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        log.info("Received stock.reserved for orderId: {}", event.getOrderId());
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                OrderStatus fromStatus = order.getStatus();
                order.setStatus(OrderStatus.STOCK_RESERVED);
                order.addStatusHistory(fromStatus, OrderStatus.STOCK_RESERVED, "Stock reserved successfully");
                orderRepository.save(order);

                // Publish payment requested
                PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("payment.requested")
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .amount(order.getTotalAmount())
                        .paymentMethod(order.getPaymentMethod().name())
                        .timestamp(Instant.now())
                        .build();
                orderEventProducer.publishPaymentRequested(paymentEvent);
            }
        });

        markAsProcessed(event.getEventId(), "stock.reserved");
    }

    @KafkaListener(topics = "stock.rejected", groupId = "order-service-group")
    @Transactional
    public void handleStockRejected(StockRejectedEvent event) {
        log.info("Received stock.rejected for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                OrderStatus fromStatus = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.addStatusHistory(fromStatus, OrderStatus.CANCELLED, "Stock rejected: " + event.getReason());
                orderRepository.save(order);

                // Notify cancellation
                OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("order.cancelled")
                        .orderId(order.getId())
                        .reason("Stock rejected: " + event.getReason())
                        .timestamp(Instant.now())
                        .build();
                orderEventProducer.publishOrderCancelled(cancelEvent);
            }
        });

        markAsProcessed(event.getEventId(), "stock.rejected");
    }

    @KafkaListener(topics = "payment.succeeded", groupId = "order-service-group")
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received payment.succeeded for orderId: {}, transactionId: {}", event.getOrderId(), event.getTransactionId());
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
                OrderStatus fromStatus = order.getStatus();
                order.setStatus(OrderStatus.CONFIRMED);
                order.addStatusHistory(fromStatus, OrderStatus.CONFIRMED, "Payment succeeded. Transaction: " + event.getTransactionId());
                orderRepository.save(order);

                // Publish order confirmed for Inventory (deduct real stock) and Notification
                OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("order.confirmed")
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .timestamp(Instant.now())
                        .build();
                orderEventProducer.publishOrderConfirmed(confirmedEvent);
            }
        });

        markAsProcessed(event.getEventId(), "payment.succeeded");
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
                OrderStatus fromStatus = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.addStatusHistory(fromStatus, OrderStatus.CANCELLED, "Payment failed: " + event.getReason());
                orderRepository.save(order);

                // Trigger compensating transaction to release reserved stock in Inventory
                OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("order.cancelled")
                        .orderId(order.getId())
                        .reason("Payment failed: " + event.getReason())
                        .timestamp(Instant.now())
                        .build();
                orderEventProducer.publishOrderCancelled(cancelEvent);
            }
        });

        markAsProcessed(event.getEventId(), "payment.failed");
    }

    private boolean isAlreadyProcessed(String eventId) {
        if (eventId == null) {
            return false;
        }
        boolean exists = processedEventRepository.existsById(eventId);
        if (exists) {
            log.warn("Event {} already processed, skipping duplicate execution (Idempotent)", eventId);
        }
        return exists;
    }

    private void markAsProcessed(String eventId, String eventType) {
        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .processedAt(Instant.now())
                    .build());
        }
    }
}
