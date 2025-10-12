package com.minishop.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minishop.paymentservice.config.VnPayConfig;
import com.minishop.paymentservice.dto.response.PaymentStatusResponse;
import com.minishop.paymentservice.dto.response.VnPayIpnResponse;
import com.minishop.paymentservice.entity.*;
import com.minishop.paymentservice.event.dto.*;
import com.minishop.paymentservice.event.producer.PaymentEventProducer;
import com.minishop.paymentservice.gateway.VnPayClient;
import com.minishop.paymentservice.mapper.PaymentMapper;
import com.minishop.paymentservice.repository.PaymentCallbackLogRepository;
import com.minishop.paymentservice.repository.PaymentTransactionRepository;
import com.minishop.paymentservice.repository.ProcessedEventRepository;
import com.minishop.paymentservice.security.SignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentCallbackLogRepository paymentCallbackLogRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private VnPayClient vnPayClient;

    @Mock
    private VnPayConfig vnPayConfig;

    @Mock
    private SignatureVerifier signatureVerifier;

    @Mock
    private PaymentMapper paymentMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private PaymentTransaction sampleTransaction;
    private PaymentStatusResponse sampleStatusResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        sampleTransaction = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .transactionCode("TXN20260814001")
                .amount(new BigDecimal("100000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .expiredAt(Instant.now().plusSeconds(900))
                .build();

        sampleStatusResponse = PaymentStatusResponse.builder()
                .id(sampleTransaction.getId())
                .orderId(orderId)
                .transactionCode(sampleTransaction.getTransactionCode())
                .amount(sampleTransaction.getAmount())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void testProcessPaymentRequestedCodSettlesImmediately() {
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(new BigDecimal("200000"))
                .paymentMethod("COD")
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentTransactionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPaymentRequested(event);

        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(paymentEventProducer).publishPaymentSucceeded(any(PaymentSucceededEvent.class));
    }

    @Test
    void testProcessPaymentRequestedVnPayGeneratesUrl() {
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(new BigDecimal("200000"))
                .paymentMethod("VNPAY")
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentTransactionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(vnPayClient.createPaymentUrl(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=123");

        paymentService.processPaymentRequested(event);

        verify(vnPayClient).createPaymentUrl(anyString(), eq(event.getAmount()), anyString());
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(paymentEventProducer, never()).publishPaymentSucceeded(any());
    }

    @Test
    void testProcessVnPayIpnSuccess() {
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_TxnRef", "TXN20260814001");
        ipnParams.put("vnp_Amount", "10000000"); // 100000.00 * 100
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TransactionNo", "14567890");
        ipnParams.put("vnp_SecureHash", "mockHash");

        when(paymentTransactionRepository.findByTransactionCode("TXN20260814001")).thenReturn(Optional.of(sampleTransaction));
        when(vnPayConfig.getHashSecret()).thenReturn("mockSecret");
        when(signatureVerifier.verifyVnPaySignature(ipnParams, "mockSecret")).thenReturn(true);

        VnPayIpnResponse response = paymentService.processVnPayIpn(ipnParams);

        assertNotNull(response);
        assertEquals("00", response.getRspCode());
        assertEquals(PaymentStatus.SUCCESS, sampleTransaction.getStatus());
        verify(paymentCallbackLogRepository).save(any(PaymentCallbackLog.class));
        verify(paymentEventProducer).publishPaymentSucceeded(any(PaymentSucceededEvent.class));
    }

    @Test
    void testProcessVnPayIpnInvalidSignatureRejects() {
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_TxnRef", "TXN20260814001");
        ipnParams.put("vnp_SecureHash", "invalidHash");

        when(paymentTransactionRepository.findByTransactionCode("TXN20260814001")).thenReturn(Optional.of(sampleTransaction));
        when(vnPayConfig.getHashSecret()).thenReturn("mockSecret");
        when(signatureVerifier.verifyVnPaySignature(ipnParams, "mockSecret")).thenReturn(false);

        VnPayIpnResponse response = paymentService.processVnPayIpn(ipnParams);

        assertNotNull(response);
        assertEquals("97", response.getRspCode()); // Invalid Checksum
        assertEquals(PaymentStatus.PENDING, sampleTransaction.getStatus()); // Status unchanged
        verify(paymentCallbackLogRepository).save(any(PaymentCallbackLog.class));
        verify(paymentEventProducer, never()).publishPaymentSucceeded(any());
    }

    @Test
    void testProcessVnPayIpnIdempotencyAlreadyConfirmed() {
        sampleTransaction.setStatus(PaymentStatus.SUCCESS);

        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_TxnRef", "TXN20260814001");
        ipnParams.put("vnp_SecureHash", "mockHash");

        when(paymentTransactionRepository.findByTransactionCode("TXN20260814001")).thenReturn(Optional.of(sampleTransaction));
        when(vnPayConfig.getHashSecret()).thenReturn("mockSecret");
        when(signatureVerifier.verifyVnPaySignature(ipnParams, "mockSecret")).thenReturn(true);

        VnPayIpnResponse response = paymentService.processVnPayIpn(ipnParams);

        assertNotNull(response);
        assertEquals("02", response.getRspCode()); // Order already confirmed
        verify(paymentEventProducer, never()).publishPaymentSucceeded(any());
    }
}
