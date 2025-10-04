package com.minishop.orderservice.controller;

import com.minishop.orderservice.dto.request.CancelOrderRequest;
import com.minishop.orderservice.dto.request.CheckoutRequest;
import com.minishop.orderservice.dto.request.UpdateOrderStatusRequest;
import com.minishop.orderservice.dto.response.OrderDetailResponse;
import com.minishop.orderservice.dto.response.OrderResponse;
import com.minishop.orderservice.dto.response.PagedResponse;
import com.minishop.orderservice.entity.OrderStatus;
import com.minishop.orderservice.security.SecurityUtils;
import com.minishop.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for checkout, order history, and Saga status management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Checkout current shopping cart and initiate Saga workflow")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        OrderResponse response = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get user order history with optional status filter and pagination")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Sort sortOrder = sort.endsWith(",asc")
                ? Sort.by(Sort.Direction.ASC, sort.split(",")[0])
                : Sort.by(Sort.Direction.DESC, sort.split(",")[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        PagedResponse<OrderResponse> response = orderService.getMyOrders(userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail by ID with audit status history")
    public ResponseEntity<OrderDetailResponse> getOrderById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        OrderDetailResponse response = orderService.getOrderById(id, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order before shipping")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        OrderDetailResponse response = orderService.cancelOrder(id, userId, isAdmin, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Admin/Seller: Advance order status in the state machine")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderDetailResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
