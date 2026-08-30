package com.portfolio.inventoryservice.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryResultEvent(
        UUID eventId,
        UUID orderId,
        Result result,
        String reason,
        Instant occurredAt,
        int eventVersion
) {
    public enum Result {
        RESERVED,
        REJECTED
    }
}