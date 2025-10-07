package com.minishop.inventoryservice.controller;

import com.minishop.inventoryservice.dto.request.AdjustStockRequest;
import com.minishop.inventoryservice.dto.request.ImportStockRequest;
import com.minishop.inventoryservice.dto.response.InventoryResponse;
import com.minishop.inventoryservice.dto.response.StockMovementResponse;
import com.minishop.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints for inventory inspection, stock imports, adjustments and audit logs")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Import stock for a product (increase total quantity)")
    public ResponseEntity<InventoryResponse> importStock(@Valid @RequestBody ImportStockRequest request) {
        InventoryResponse response = inventoryService.importStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Operation(summary = "Get inventory status for a specific product")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Manually adjust product stock with audit note")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request
    ) {
        InventoryResponse response = inventoryService.adjustStock(productId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: View complete append-only stock movement audit trail for a product")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(@PathVariable UUID productId) {
        List<StockMovementResponse> movements = inventoryService.getStockMovements(productId);
        return ResponseEntity.ok(movements);
    }
}
