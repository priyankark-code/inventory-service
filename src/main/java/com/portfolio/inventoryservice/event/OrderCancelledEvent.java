package com.portfolio.inventoryservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt,
        int eventVersion
) {
}