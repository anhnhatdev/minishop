package com.minishop.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minishop.apigateway.dto.GatewayErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final SecretKey secretKey;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AuthenticationFilter(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret
    ) {
        super(Config.class);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                // Enrich request headers before forwarding
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Role", role != null ? role : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (JwtException | IllegalArgumentException ex) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token: " + ex.getMessage());
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse errorResponse = GatewayErrorResponse.builder()
                .timestamp(Instant.now())
                .status(httpStatus.value())
                .error(httpStatus.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties if needed in future
    }
}
