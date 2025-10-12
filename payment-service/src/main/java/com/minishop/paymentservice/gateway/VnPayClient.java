package com.minishop.paymentservice.gateway;

import com.minishop.paymentservice.config.VnPayConfig;
import com.minishop.paymentservice.security.SignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class VnPayClient {

    private final VnPayConfig vnPayConfig;
    private final SignatureVerifier signatureVerifier;

    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String createPaymentUrl(String transactionCode, BigDecimal amount, String ipAddress) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String vnpCreateDate = now.format(VNP_DATE_FORMAT);
        String vnpExpireDate = now.plusMinutes(15).format(VNP_DATE_FORMAT);

        // VNPay amount is represented in smallest currency unit (multiply by 100)
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        vnpParams.put("vnp_TxnRef", transactionCode);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + transactionCode);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", (ipAddress != null && !ipAddress.isEmpty()) ? ipAddress : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Sort keys
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnpSecureHash = signatureVerifier.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        return vnPayConfig.getPayUrl() + "?" + queryUrl;
    }
}
