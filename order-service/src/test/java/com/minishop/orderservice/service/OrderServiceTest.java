package com.minishop.orderservice.service;

import com.minishop.orderservice.dto.request.CancelOrderRequest;
import com.minishop.orderservice.dto.request.CheckoutRequest;
import com.minishop.orderservice.dto.request.UpdateOrderStatusRequest;
import com.minishop.orderservice.dto.response.OrderDetailResponse;
import com.minishop.orderservice.dto.response.OrderResponse;
import com.minishop.orderservice.entity.*;
import com.minishop.orderservice.event.dto.OrderCreatedEvent;
import com.minishop.orderservice.event.producer.OrderEventProducer;
import com.minishop.orderservice.exception.InvalidOrderStatusTransitionException;
import com.minishop.orderservice.exception.OrderNotFoundException;
import com.minishop.orderservice.mapper.OrderMapper;
import com.minishop.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private Cart sampleCart;
    private CartItem sampleCartItem;
    private Order sampleOrder;
    private OrderResponse sampleOrderResponse;
    private OrderDetailResponse sampleOrderDetailResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        sampleCart = Cart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(new ArrayList<>())
                .build();

        sampleCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(sampleCart)
                .productId(UUID.randomUUID())
                .productName("Áo thun basic")
                .price(new BigDecimal("199000"))
                .quantity(2)
                .build();

        sampleCart.getItems().add(sampleCartItem);

        sampleOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderCode("ORD202608140001")
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("398000"))
                .shippingAddress("123 Nguyen Hue")
                .shippingPhone("0901234567")
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        sampleOrderResponse = OrderResponse.builder()
                .id(sampleOrder.getId())
                .orderCode(sampleOrder.getOrderCode())
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(sampleOrder.getTotalAmount())
                .paymentMethod(PaymentMethod.COD)
                .itemCount(2)
                .build();

        sampleOrderDetailResponse = OrderDetailResponse.builder()
                .id(sampleOrder.getId())
                .orderCode(sampleOrder.getOrderCode())
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(sampleOrder.getTotalAmount())
                .shippingAddress("123 Nguyen Hue")
                .shippingPhone("0901234567")
                .paymentMethod(PaymentMethod.COD)
                .build();
    }

    @Test
    void testCheckoutSuccess() {
        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Nguyen Hue")
                .shippingPhone("0901234567")
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(cartService.getOrCreateCartEntity(userId)).thenReturn(sampleCart);
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
        when(orderMapper.toOrderResponse(sampleOrder)).thenReturn(sampleOrderResponse);

        OrderResponse response = orderService.checkout(userId, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(cartService).clearCart(userId);
        verify(orderEventProducer).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void testCheckoutEmptyCartThrowsException() {
        sampleCart.getItems().clear();
        when(cartService.getOrCreateCartEntity(userId)).thenReturn(sampleCart);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Nguyen Hue")
                .shippingPhone("0901234567")
                .paymentMethod(PaymentMethod.COD)
                .build();

        assertThrows(IllegalStateException.class, () -> orderService.checkout(userId, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrderSuccess() {
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
        when(orderMapper.toOrderDetailResponse(sampleOrder)).thenReturn(sampleOrderDetailResponse);

        CancelOrderRequest request = CancelOrderRequest.builder().reason("Change mind").build();
        OrderDetailResponse response = orderService.cancelOrder(sampleOrder.getId(), userId, false, request);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, sampleOrder.getStatus());
        verify(orderEventProducer).publishOrderCancelled(any());
    }

    @Test
    void testInvalidStatusTransitionThrowsException() {
        sampleOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.PENDING) // Cannot go backwards to PENDING
                .build();

        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> orderService.updateOrderStatus(sampleOrder.getId(), request));
    }

    @Test
    void testGetOrderByIdNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(id, userId, false));
    }
}
