package com.minishop.inventoryservice.mapper;

import com.minishop.inventoryservice.dto.response.InventoryResponse;
import com.minishop.inventoryservice.dto.response.StockMovementResponse;
import com.minishop.inventoryservice.dto.response.StockReservationResponse;
import com.minishop.inventoryservice.entity.Inventory;
import com.minishop.inventoryservice.entity.StockMovement;
import com.minishop.inventoryservice.entity.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "availableQuantity", expression = "java(inventory.getAvailableQuantity())")
    InventoryResponse toInventoryResponse(Inventory inventory);

    StockMovementResponse toStockMovementResponse(StockMovement stockMovement);

    List<StockMovementResponse> toStockMovementResponseList(List<StockMovement> stockMovements);

    StockReservationResponse toStockReservationResponse(StockReservation stockReservation);

    List<StockReservationResponse> toStockReservationResponseList(List<StockReservation> stockReservations);
}
