package com.minishop.orderservice.client;

import com.minishop.orderservice.client.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductServiceClient {

    @GetMapping("/{id}")
    ProductDto getProductById(@PathVariable("id") UUID id);
}
