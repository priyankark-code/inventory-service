package com.portfolio.inventoryservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryReservationEntity() {
        // Required by JPA
    }

    public InventoryReservationEntity(
            UUID id,
            UUID orderId,
            String productId,
            int quantity,
            String status,
            Instant createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public void release() {
        if ("RELEASED".equals(status)) {
            return;
        }

        if (!"RESERVED".equals(status)) {
            throw new IllegalStateException(
                    "Reservation " + id
                            + " cannot be released from status "
                            + status
            );
        }

        status = "RELEASED";
    }
}