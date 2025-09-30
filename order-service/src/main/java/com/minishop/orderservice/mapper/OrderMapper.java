package com.minishop.orderservice.mapper;

import com.minishop.orderservice.dto.response.OrderDetailResponse;
import com.minishop.orderservice.dto.response.OrderItemResponse;
import com.minishop.orderservice.dto.response.OrderResponse;
import com.minishop.orderservice.dto.response.OrderStatusHistoryResponse;
import com.minishop.orderservice.entity.Order;
import com.minishop.orderservice.entity.OrderItem;
import com.minishop.orderservice.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "itemCount", source = "items", qualifiedByName = "countItems")
    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponseList(List<Order> orders);

    @Mapping(target = "items", source = "items")
    @Mapping(target = "statusHistory", source = "statusHistory")
    OrderDetailResponse toOrderDetailResponse(Order order);

    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory history);

    @Named("countItems")
    default Integer countItems(List<OrderItem> items) {
        if (items == null) {
            return 0;
        }
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
