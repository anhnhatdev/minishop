package com.minishop.paymentservice.mapper;

import com.minishop.paymentservice.dto.response.PaymentCallbackLogResponse;
import com.minishop.paymentservice.dto.response.PaymentStatusResponse;
import com.minishop.paymentservice.entity.PaymentCallbackLog;
import com.minishop.paymentservice.entity.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentStatusResponse toPaymentStatusResponse(PaymentTransaction transaction);

    @Mapping(target = "transactionId", source = "transaction.id")
    PaymentCallbackLogResponse toPaymentCallbackLogResponse(PaymentCallbackLog callbackLog);

    List<PaymentCallbackLogResponse> toPaymentCallbackLogResponseList(List<PaymentCallbackLog> callbackLogs);
}
