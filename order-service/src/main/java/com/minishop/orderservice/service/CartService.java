package com.minishop.orderservice.service;

import com.minishop.orderservice.client.ProductServiceClient;
import com.minishop.orderservice.client.dto.ProductDto;
import com.minishop.orderservice.dto.request.AddToCartRequest;
import com.minishop.orderservice.dto.request.UpdateCartItemRequest;
import com.minishop.orderservice.dto.response.CartResponse;
import com.minishop.orderservice.entity.Cart;
import com.minishop.orderservice.entity.CartItem;
import com.minishop.orderservice.exception.CartNotFoundException;
import com.minishop.orderservice.mapper.CartMapper;
import com.minishop.orderservice.repository.CartItemRepository;
import com.minishop.orderservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;
    private final CartMapper cartMapper;

    @Transactional
    public Cart getOrCreateCartEntity(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        // Synchronous call to product-service to get latest product snapshot & verify availability
        ProductDto product = productServiceClient.getProductById(request.getProductId());
        if (product == null || !"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Product is not available for purchase");
        }

        Cart cart = getOrCreateCartEntity(userId);

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            // Update snapshot price to latest
            existingItem.setPrice(product.getPrice());
            existingItem.setProductName(product.getName());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cart.addItem(newItem);
            cartRepository.save(cart);
        }

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartNotFoundException("Cart item not found with ID: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to user's cart");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse removeCartItem(UUID userId, UUID itemId) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartNotFoundException("Cart item not found with ID: " + itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to user's cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }
}
