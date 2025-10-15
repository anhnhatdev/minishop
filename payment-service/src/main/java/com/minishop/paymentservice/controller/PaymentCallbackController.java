package com.minishop.paymentservice.controller;

import com.minishop.paymentservice.dto.response.PaymentStatusResponse;
import com.minishop.paymentservice.dto.response.VnPayIpnResponse;
import com.minishop.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Gateway Callbacks", description = "Endpoints for VNPay browser returns and server-to-server IPN webhooks")
public class PaymentCallbackController {

    private final PaymentService paymentService;

    /**
     * Non-authoritative Return URL: VNPay redirects user browser here.
     * Only displays current status to user; NEVER alters financial states.
     */
    @GetMapping("/return")
    @Operation(summary = "Non-authoritative return page for frontend user redirect")
    public ResponseEntity<PaymentStatusResponse> handleReturn(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay Return URL redirect with params: {}", allParams);
        PaymentStatusResponse response = paymentService.handleVnPayReturn(allParams);
        return ResponseEntity.ok(response);
    }

    /**
     * Authoritative IPN URL: VNPay server calls our backend directly.
     */
    @GetMapping("/ipn")
    @Operation(summary = "Authoritative VNPay IPN Webhook (GET)")
    public ResponseEntity<VnPayIpnResponse> handleIpnGet(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay IPN GET webhook with params: {}", allParams);
        VnPayIpnResponse response = paymentService.processVnPayIpn(allParams);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ipn")
    @Operation(summary = "Authoritative VNPay IPN Webhook (POST)")
    public ResponseEntity<VnPayIpnResponse> handleIpnPost(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay IPN POST webhook with params: {}", allParams);
        VnPayIpnResponse response = paymentService.processVnPayIpn(allParams);
        return ResponseEntity.ok(response);
    }
}
