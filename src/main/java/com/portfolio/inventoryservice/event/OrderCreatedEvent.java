package com.portfolio.inventoryservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        List<Item> items,
        BigDecimal totalAmount,
        Instant occurredAt,
        int eventVersion
) {
    public record Item(
            String productId,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}