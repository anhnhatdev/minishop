package com.minishop.orderservice.mapper;

import com.minishop.orderservice.dto.response.CartItemResponse;
import com.minishop.orderservice.dto.response.CartResponse;
import com.minishop.orderservice.entity.Cart;
import com.minishop.orderservice.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "totalAmount", source = "items", qualifiedByName = "calculateTotal")
    CartResponse toCartResponse(Cart cart);

    @Mapping(target = "subtotal", expression = "java(item.getSubtotal())")
    CartItemResponse toCartItemResponse(CartItem item);

    List<CartItemResponse> toCartItemResponseList(List<CartItem> items);

    @Named("calculateTotal")
    default BigDecimal calculateTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
