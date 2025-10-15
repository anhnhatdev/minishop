package com.minishop.paymentservice.controller;

import com.minishop.paymentservice.dto.response.PaymentCallbackLogResponse;
import com.minishop.paymentservice.dto.response.PaymentStatusResponse;
import com.minishop.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for payment status polling and audit logs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get payment transaction status and payment URL for an order")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable UUID orderId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/callback-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: View raw gateway callback audit logs for dispute reconciliation")
    public ResponseEntity<List<PaymentCallbackLogResponse>> getCallbackLogs(@PathVariable UUID orderId) {
        List<PaymentCallbackLogResponse> logs = paymentService.getCallbackLogs(orderId);
        return ResponseEntity.ok(logs);
    }
}
