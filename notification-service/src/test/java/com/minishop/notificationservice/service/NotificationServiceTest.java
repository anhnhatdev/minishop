package com.minishop.notificationservice.service;

import com.minishop.notificationservice.dto.response.ResendNotificationResponse;
import com.minishop.notificationservice.entity.*;
import com.minishop.notificationservice.exception.TemplateNotFoundException;
import com.minishop.notificationservice.mapper.NotificationMapper;
import com.minishop.notificationservice.repository.NotificationLogRepository;
import com.minishop.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private TemplateRenderService renderService;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private SmsSenderService smsSenderService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationTemplate orderConfirmedTemplate;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderConfirmedTemplate = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .code("ORDER_CONFIRMED")
                .channel(NotificationChannel.EMAIL)
                .subject("Xác nhận đơn #{{orderCode}}")
                .bodyTemplate("Chào {{customerName}}, đơn hàng {{orderCode}} đã thành công.")
                .build();
    }

    @Test
    void testSendEmailNotificationSuccess() throws Exception {
        when(templateRepository.findByCodeAndChannel("ORDER_CONFIRMED", NotificationChannel.EMAIL))
                .thenReturn(Optional.of(orderConfirmedTemplate));

        when(renderService.render(eq("Xác nhận đơn #{{orderCode}}"), any()))
                .thenReturn("Xác nhận đơn #ORD001");
        when(renderService.render(eq("Chào {{customerName}}, đơn hàng {{orderCode}} đã thành công."), any()))
                .thenReturn("Chào Nguyễn A, đơn hàng ORD001 đã thành công.");

        when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationLog result = notificationService.sendNotification(
                userId,
                NotificationChannel.EMAIL,
                "a@example.com",
                "ORDER_CONFIRMED",
                Map.of("customerName", "Nguyễn A", "orderCode", "ORD001"),
                "ORD001"
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertNotNull(result.getSentAt());
        assertNull(result.getErrorMessage());

        verify(emailSenderService).sendHtmlEmail(eq("a@example.com"), eq("Xác nhận đơn #ORD001"), anyString());
        verify(logRepository, atLeast(2)).save(any(NotificationLog.class));
    }

    @Test
    void testSendEmailNotificationFailureCapturesErrorWithoutThrowing() throws Exception {
        when(templateRepository.findByCodeAndChannel("ORDER_CONFIRMED", NotificationChannel.EMAIL))
                .thenReturn(Optional.of(orderConfirmedTemplate));

        when(renderService.render(anyString(), any())).thenReturn("Rendered content");
        doThrow(new RuntimeException("SMTP connection timeout")).when(emailSenderService).sendHtmlEmail(any(), any(), any());
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationLog result = notificationService.sendNotification(
                userId,
                NotificationChannel.EMAIL,
                "a@example.com",
                "ORDER_CONFIRMED",
                Map.of(),
                "ORD001"
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertEquals("SMTP connection timeout", result.getErrorMessage());
    }

    @Test
    void testSendNotificationTemplateNotFoundThrowsException() {
        when(templateRepository.findByCodeAndChannel("UNKNOWN_CODE", NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> notificationService.sendNotification(
                userId,
                NotificationChannel.EMAIL,
                "a@example.com",
                "UNKNOWN_CODE",
                Map.of(),
                "REF123"
        ));
    }

    @Test
    void testResendNotificationIncrementsRetryCount() throws Exception {
        UUID logId = UUID.randomUUID();
        NotificationLog existingLog = NotificationLog.builder()
                .id(logId)
                .channel(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .renderedSubject("Subj")
                .renderedContent("Content")
                .status(NotificationStatus.FAILED)
                .retryCount(0)
                .build();

        when(logRepository.findById(logId)).thenReturn(Optional.of(existingLog));
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResendNotificationResponse response = notificationService.resendNotification(logId);

        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals(1, response.getRetryCount());
        verify(emailSenderService).sendHtmlEmail(eq("test@example.com"), eq("Subj"), eq("Content"));
    }
}
