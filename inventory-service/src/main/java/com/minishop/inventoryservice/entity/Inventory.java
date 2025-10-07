package com.minishop.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return Math.max(0, totalQuantity - reservedQuantity);
    }

    public boolean canReserve(int quantity) {
        return getAvailableQuantity() >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock to reserve. Available: " + getAvailableQuantity() + ", requested: " + quantity);
        }
        this.reservedQuantity += quantity;
    }

    public void deduct(int quantity) {
        if (this.reservedQuantity < quantity || this.totalQuantity < quantity) {
            throw new IllegalStateException("Cannot deduct stock. Total: " + totalQuantity + ", reserved: " + reservedQuantity + ", deduct: " + quantity);
        }
        this.totalQuantity -= quantity;
        this.reservedQuantity -= quantity;
    }

    public void release(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }

    public void importStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Import quantity must be positive");
        }
        this.totalQuantity += quantity;
    }

    public void adjust(int change) {
        if (this.totalQuantity + change < this.reservedQuantity) {
            throw new IllegalStateException("Total quantity cannot be less than currently reserved quantity");
        }
        this.totalQuantity += change;
    }
}
