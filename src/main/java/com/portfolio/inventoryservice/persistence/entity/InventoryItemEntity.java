package com.portfolio.inventoryservice.persistence.entity;

import com.portfolio.inventoryservice.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    @Column(nullable = false)
    private long version;

    protected InventoryItemEntity() {
        // Required by JPA
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be positive"
            );
        }

        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                    productId,
                    quantity,
                    availableQuantity
            );
        }

        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }
}