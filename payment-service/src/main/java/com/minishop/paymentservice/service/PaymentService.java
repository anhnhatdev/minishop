package com.minishop.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minishop.paymentservice.config.VnPayConfig;
import com.minishop.paymentservice.dto.response.PaymentCallbackLogResponse;
import com.minishop.paymentservice.dto.response.PaymentStatusResponse;
import com.minishop.paymentservice.dto.response.VnPayIpnResponse;
import com.minishop.paymentservice.entity.*;
import com.minishop.paymentservice.event.dto.PaymentFailedEvent;
import com.minishop.paymentservice.event.dto.PaymentRequestedEvent;
import com.minishop.paymentservice.event.dto.PaymentSucceededEvent;
import com.minishop.paymentservice.event.producer.PaymentEventProducer;
import com.minishop.paymentservice.exception.PaymentTransactionNotFoundException;
import com.minishop.paymentservice.gateway.VnPayClient;
import com.minishop.paymentservice.mapper.PaymentMapper;
import com.minishop.paymentservice.repository.PaymentCallbackLogRepository;
import com.minishop.paymentservice.repository.PaymentTransactionRepository;
import com.minishop.paymentservice.repository.ProcessedEventRepository;
import com.minishop.paymentservice.security.SignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentCallbackLogRepository paymentCallbackLogRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final VnPayClient vnPayClient;
    private final VnPayConfig vnPayConfig;
    private final SignatureVerifier signatureVerifier;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TXN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Random random = new Random();

    @Transactional
    public void processPaymentRequested(PaymentRequestedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        log.info("Processing payment.requested for orderId: {}, amount: {}, method: {}",
                event.getOrderId(), event.getAmount(), event.getPaymentMethod());

        // Check if transaction already exists for this order
        Optional<PaymentTransaction> existingOpt = paymentTransactionRepository.findByOrderId(event.getOrderId());
        if (existingOpt.isPresent()) {
            log.info("Payment transaction already exists for order: {}", event.getOrderId());
            markAsProcessed(event.getEventId(), "payment.requested");
            return;
        }

        String transactionCode = generateTransactionCode();
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(event.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.COD;
        }

        if (method == PaymentMethod.COD) {
            // Cash On Delivery settles immediately as committed
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(event.getOrderId())
                    .transactionCode(transactionCode)
                    .amount(event.getAmount())
                    .paymentMethod(PaymentMethod.COD)
                    .status(PaymentStatus.SUCCESS)
                    .paidAt(Instant.now())
                    .expiredAt(Instant.now().plusSeconds(15 * 60))
                    .build();

            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            markAsProcessed(event.getEventId(), "payment.requested");

            PaymentSucceededEvent succeededEvent = PaymentSucceededEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("payment.succeeded")
                    .orderId(saved.getOrderId())
                    .transactionId(saved.getTransactionCode())
                    .timestamp(Instant.now())
                    .build();
            paymentEventProducer.publishPaymentSucceeded(succeededEvent);

        } else if (method == PaymentMethod.VNPAY) {
            String clientIp = (event.getClientIp() != null && !event.getClientIp().isBlank()) ? event.getClientIp() : "127.0.0.1";
            String paymentUrl = vnPayClient.createPaymentUrl(transactionCode, event.getAmount(), clientIp);

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(event.getOrderId())
                    .transactionCode(transactionCode)
                    .amount(event.getAmount())
                    .paymentMethod(PaymentMethod.VNPAY)
                    .status(PaymentStatus.PENDING)
                    .paymentUrl(paymentUrl)
                    .expiredAt(Instant.now().plusSeconds(15 * 60))
                    .build();

            paymentTransactionRepository.save(transaction);
            markAsProcessed(event.getEventId(), "payment.requested");
        } else {
            // MOMO or default fallback
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(event.getOrderId())
                    .transactionCode(transactionCode)
                    .amount(event.getAmount())
                    .paymentMethod(method)
                    .status(PaymentStatus.PENDING)
                    .expiredAt(Instant.now().plusSeconds(15 * 60))
                    .build();

            paymentTransactionRepository.save(transaction);
            markAsProcessed(event.getEventId(), "payment.requested");
        }
    }

    /**
     * Authoritative Server-to-Server IPN Webhook callback processing.
     */
    @Transactional
    public VnPayIpnResponse processVnPayIpn(Map<String, String> allParams) {
        String rawPayload = "";
        try {
            rawPayload = objectMapper.writeValueAsString(allParams);
        } catch (Exception ignored) {
            rawPayload = allParams.toString();
        }

        String txnRef = allParams.get("vnp_TxnRef");
        Optional<PaymentTransaction> transactionOpt = (txnRef != null)
                ? paymentTransactionRepository.findByTransactionCode(txnRef)
                : Optional.empty();

        // 1. Always record raw payload in payment_callback_logs before validation
        PaymentCallbackLog callbackLog = PaymentCallbackLog.builder()
                .transaction(transactionOpt.orElse(null))
                .rawPayload(rawPayload)
                .signatureValid(false)
                .processed(false)
                .receivedAt(Instant.now())
                .build();

        // 2. Verify HMAC-SHA512 signature
        boolean isValidSignature = signatureVerifier.verifyVnPaySignature(allParams, vnPayConfig.getHashSecret());
        callbackLog.setSignatureValid(isValidSignature);

        if (!isValidSignature) {
            paymentCallbackLogRepository.save(callbackLog);
            log.warn("Invalid VNPay IPN signature for txnRef: {}", txnRef);
            return VnPayIpnResponse.invalidSignature();
        }

        if (transactionOpt.isEmpty()) {
            paymentCallbackLogRepository.save(callbackLog);
            log.warn("VNPay IPN transaction not found: {}", txnRef);
            return VnPayIpnResponse.orderNotFound();
        }

        PaymentTransaction transaction = transactionOpt.get();

        // 3. Idempotency check: Already processed transaction
        if (transaction.getStatus() != PaymentStatus.PENDING && transaction.getStatus() != PaymentStatus.INITIATED) {
            callbackLog.setProcessed(true);
            paymentCallbackLogRepository.save(callbackLog);
            log.info("VNPay IPN already processed for transaction: {}", txnRef);
            return VnPayIpnResponse.orderAlreadyConfirmed();
        }

        // 4. Validate Amount
        String vnpAmountStr = allParams.get("vnp_Amount");
        if (vnpAmountStr != null) {
            long vnpAmount = Long.parseLong(vnpAmountStr);
            long expectedAmount = transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            if (vnpAmount != expectedAmount) {
                paymentCallbackLogRepository.save(callbackLog);
                log.warn("VNPay IPN amount mismatch: expected={}, received={}", expectedAmount, vnpAmount);
                return VnPayIpnResponse.invalidAmount();
            }
        }

        // 5. Check response code & update status
        String responseCode = allParams.get("vnp_ResponseCode");
        String gatewayTransactionId = allParams.get("vnp_TransactionNo");

        transaction.setGatewayResponseCode(responseCode);
        transaction.setGatewayTransactionId(gatewayTransactionId);

        if ("00".equals(responseCode)) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setPaidAt(Instant.now());
            paymentTransactionRepository.save(transaction);

            callbackLog.setProcessed(true);
            paymentCallbackLogRepository.save(callbackLog);

            // Publish payment succeeded event for Order Service
            PaymentSucceededEvent succeededEvent = PaymentSucceededEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("payment.succeeded")
                    .orderId(transaction.getOrderId())
                    .transactionId(gatewayTransactionId != null ? gatewayTransactionId : transaction.getTransactionCode())
                    .timestamp(Instant.now())
                    .build();
            paymentEventProducer.publishPaymentSucceeded(succeededEvent);

            log.info("VNPay IPN payment succeeded for orderId: {}", transaction.getOrderId());
            return VnPayIpnResponse.success();
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);

            callbackLog.setProcessed(true);
            paymentCallbackLogRepository.save(callbackLog);

            // Publish payment failed event (triggers Saga rollback)
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("payment.failed")
                    .orderId(transaction.getOrderId())
                    .reason("VNPay transaction failed with response code: " + responseCode)
                    .timestamp(Instant.now())
                    .build();
            paymentEventProducer.publishPaymentFailed(failedEvent);

            log.warn("VNPay IPN payment failed for orderId: {}, code: {}", transaction.getOrderId(), responseCode);
            return VnPayIpnResponse.success();
        }
    }

    /**
     * Non-authoritative Return URL handling for frontend user display.
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse handleVnPayReturn(Map<String, String> allParams) {
        String txnRef = allParams.get("vnp_TxnRef");
        if (txnRef == null) {
            throw new IllegalArgumentException("Missing vnp_TxnRef parameter");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionCode(txnRef)
                .orElseThrow(() -> new PaymentTransactionNotFoundException("Transaction not found for code: " + txnRef));

        return paymentMapper.toPaymentStatusResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID orderId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentTransactionNotFoundException("Payment transaction not found for order: " + orderId));
        return paymentMapper.toPaymentStatusResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<PaymentCallbackLogResponse> getCallbackLogs(UUID orderId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentTransactionNotFoundException("Payment transaction not found for order: " + orderId));

        List<PaymentCallbackLog> logs = paymentCallbackLogRepository.findByTransactionIdOrderByReceivedAtDesc(transaction.getId());
        return paymentMapper.toPaymentCallbackLogResponseList(logs);
    }

    @Transactional
    public void scanAndExpireStuckTransactions() {
        List<PaymentTransaction> expiredList = paymentTransactionRepository.findByStatusInAndExpiredAtBefore(
                List.of(PaymentStatus.INITIATED, PaymentStatus.PENDING), Instant.now()
        );

        if (!expiredList.isEmpty()) {
            log.info("Found {} expired payment transactions. Expiring and notifying Saga...", expiredList.size());
            for (PaymentTransaction transaction : expiredList) {
                transaction.setStatus(PaymentStatus.EXPIRED);
                paymentTransactionRepository.save(transaction);

                PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("payment.failed")
                        .orderId(transaction.getOrderId())
                        .reason("Payment expired after 15 minutes without completed callback")
                        .timestamp(Instant.now())
                        .build();
                paymentEventProducer.publishPaymentFailed(failedEvent);
            }
        }
    }

    private String generateTransactionCode() {
        String datePart = LocalDateTime.now().format(TXN_DATE_FORMAT);
        int randomPart = 1000 + random.nextInt(9000);
        return "TXN" + datePart + randomPart;
    }

    private boolean isAlreadyProcessed(String eventId) {
        if (eventId == null) return false;
        boolean exists = processedEventRepository.existsById(eventId);
        if (exists) {
            log.warn("Event {} already processed in payment-service, skipping duplicate", eventId);
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
