package com.minishop.reviewservice.client;

import com.minishop.reviewservice.client.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{orderId}")
    OrderDto getOrderById(@PathVariable("orderId") UUID orderId);
}
