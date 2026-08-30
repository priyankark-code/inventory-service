package com.portfolio.inventoryservice.outbox;

import com.portfolio.inventoryservice.persistence.entity.OutboxEventEntity;
import com.portfolio.inventoryservice.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxEventStateService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventStateService(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void markPublished(UUID eventId, Instant publishedAt) {
        OutboxEventEntity event = outboxEventRepository
                .findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + eventId
                ));

        event.markPublished(publishedAt);
    }
}