package com.minishop.orderservice.scheduler;

import com.minishop.orderservice.entity.Order;
import com.minishop.orderservice.entity.OrderStatus;
import com.minishop.orderservice.event.dto.OrderCancelledEvent;
import com.minishop.orderservice.event.producer.OrderEventProducer;
import com.minishop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    // Scan every 60 seconds
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scanAndCancelExpiredOrders() {
        // Orders in STOCK_RESERVED older than 15 minutes
        Instant timeoutThreshold = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.STOCK_RESERVED, timeoutThreshold);

        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired STOCK_RESERVED orders. Cancelling and triggering compensation...", expiredOrders.size());
            for (Order order : expiredOrders) {
                OrderStatus fromStatus = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.addStatusHistory(fromStatus, OrderStatus.CANCELLED, "Auto cancelled due to payment timeout (15 minutes)");
                orderRepository.save(order);

                OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("order.cancelled")
                        .orderId(order.getId())
                        .reason("Payment timeout exceeded 15 minutes")
                        .timestamp(Instant.now())
                        .build();

                orderEventProducer.publishOrderCancelled(cancelEvent);
            }
        }
    }
}
