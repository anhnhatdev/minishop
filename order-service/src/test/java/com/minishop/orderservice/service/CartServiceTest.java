package com.minishop.orderservice.service;

import com.minishop.orderservice.client.ProductServiceClient;
import com.minishop.orderservice.client.dto.ProductDto;
import com.minishop.orderservice.dto.request.AddToCartRequest;
import com.minishop.orderservice.dto.response.CartResponse;
import com.minishop.orderservice.entity.Cart;
import com.minishop.orderservice.entity.CartItem;
import com.minishop.orderservice.mapper.CartMapper;
import com.minishop.orderservice.repository.CartItemRepository;
import com.minishop.orderservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private Cart sampleCart;
    private ProductDto sampleProductDto;
    private CartResponse sampleCartResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        sampleCart = Cart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(new ArrayList<>())
                .build();

        sampleProductDto = ProductDto.builder()
                .id(UUID.randomUUID())
                .name("Áo thun basic")
                .price(new BigDecimal("199000"))
                .status("ACTIVE")
                .build();

        sampleCartResponse = CartResponse.builder()
                .id(sampleCart.getId())
                .userId(userId)
                .totalAmount(new BigDecimal("199000"))
                .build();
    }

    @Test
    void testAddToCartSuccess() {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(sampleProductDto.getId())
                .quantity(1)
                .build();

        when(productServiceClient.getProductById(sampleProductDto.getId())).thenReturn(sampleProductDto);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemRepository.findByCartIdAndProductId(sampleCart.getId(), sampleProductDto.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toCartResponse(sampleCart)).thenReturn(sampleCartResponse);

        CartResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        verify(productServiceClient).getProductById(sampleProductDto.getId());
        verify(cartRepository).save(sampleCart);
    }

    @Test
    void testAddToCartInactiveProductThrowsException() {
        sampleProductDto.setStatus("HIDDEN");
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(sampleProductDto.getId())
                .quantity(1)
                .build();

        when(productServiceClient.getProductById(sampleProductDto.getId())).thenReturn(sampleProductDto);

        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(userId, request));
    }
}
