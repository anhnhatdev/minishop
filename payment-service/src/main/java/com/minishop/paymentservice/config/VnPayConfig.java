package com.minishop.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VnPayConfig {

    private String tmnCode = "DEMOTMN1";
    private String hashSecret = "RAOANNK01UMOU64R04J1Q17TGWY2QNWG";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:8080/api/v1/payments/vnpay/return";
    private String ipnUrl = "http://localhost:8080/api/v1/payments/vnpay/ipn";
    private String version = "2.1.0";
    private String command = "pay";
    private String currCode = "VND";
    private String locale = "vn";
}
