package com.minishop.apigateway.controller;

import com.minishop.apigateway.dto.GatewayErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user-service")
    public Mono<ResponseEntity<GatewayErrorResponse>> userServiceFallback(ServerWebExchange exchange) {
        GatewayErrorResponse response = GatewayErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("User Service is temporarily unavailable. Please try again later.")
                .path(exchange.getRequest().getURI().getPath())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/product-service")
    public Mono<ResponseEntity<GatewayErrorResponse>> productServiceFallback(ServerWebExchange exchange) {
        GatewayErrorResponse response = GatewayErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("Product Service is temporarily unavailable. Please try again later.")
                .path(exchange.getRequest().getURI().getPath())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
