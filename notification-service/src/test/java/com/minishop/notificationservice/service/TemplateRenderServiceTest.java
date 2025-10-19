package com.minishop.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRenderServiceTest {

    private TemplateRenderService renderService;

    @BeforeEach
    void setUp() {
        renderService = new TemplateRenderService();
    }

    @Test
    void testRenderWithAllPlaceholdersPresent() {
        String template = "Chào {{customerName}}, đơn hàng {{orderCode}} trị giá {{totalAmount}}đ đã được xác nhận.";
        Map<String, Object> params = new HashMap<>();
        params.put("customerName", "Nguyễn Văn A");
        params.put("orderCode", "ORD12345");
        params.put("totalAmount", "590000");

        String result = renderService.render(template, params);

        assertEquals("Chào Nguyễn Văn A, đơn hàng ORD12345 trị giá 590000đ đã được xác nhận.", result);
    }

    @Test
    void testRenderWithMissingPlaceholderReplacesWithEmpty() {
        String template = "Chào {{customerName}}, mã tracking của bạn là: {{trackingNumber}}.";
        Map<String, Object> params = new HashMap<>();
        params.put("customerName", "Trần B");

        String result = renderService.render(template, params);

        assertEquals("Chào Trần B, mã tracking của bạn là: .", result);
    }

    @Test
    void testRenderWithNullOrEmptyParamsReturnsTemplate() {
        String template = "Thông báo hệ thống chung.";

        String resultNull = renderService.render(template, null);
        String resultEmpty = renderService.render(template, Map.of());

        assertEquals("Thông báo hệ thống chung.", resultNull);
        assertEquals("Thông báo hệ thống chung.", resultEmpty);
    }

    @Test
    void testRenderWithNullTemplateReturnsEmptyString() {
        String result = renderService.render(null, Map.of("key", "val"));
        assertEquals("", result);
    }
}
