package com.minishop.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCallbackLogResponse {

    private UUID id;
    private UUID transactionId;
    private String rawPayload;
    private Boolean signatureValid;
    private Boolean processed;
    private Instant receivedAt;
}
