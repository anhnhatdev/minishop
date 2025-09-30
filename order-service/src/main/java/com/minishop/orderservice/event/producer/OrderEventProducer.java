package com.minishop.orderservice.event.producer;

import com.minishop.orderservice.event.dto.OrderCancelledEvent;
import com.minishop.orderservice.event.dto.OrderConfirmedEvent;
import com.minishop.orderservice.event.dto.OrderCreatedEvent;
import com.minishop.orderservice.event.dto.PaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_ORDER_CREATED = "order.created";
    public static final String TOPIC_PAYMENT_REQUESTED = "payment.requested";
    public static final String TOPIC_ORDER_CONFIRMED = "order.confirmed";
    public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing {} for orderId: {}", TOPIC_ORDER_CREATED, event.getOrderId());
        kafkaTemplate.send(TOPIC_ORDER_CREATED, event.getOrderId().toString(), event);
    }

    public void publishPaymentRequested(PaymentRequestedEvent event) {
        log.info("Publishing {} for orderId: {}", TOPIC_PAYMENT_REQUESTED, event.getOrderId());
        kafkaTemplate.send(TOPIC_PAYMENT_REQUESTED, event.getOrderId().toString(), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing {} for orderId: {}", TOPIC_ORDER_CONFIRMED, event.getOrderId());
        kafkaTemplate.send(TOPIC_ORDER_CONFIRMED, event.getOrderId().toString(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing {} for orderId: {}", TOPIC_ORDER_CANCELLED, event.getOrderId());
        kafkaTemplate.send(TOPIC_ORDER_CANCELLED, event.getOrderId().toString(), event);
    }
}
