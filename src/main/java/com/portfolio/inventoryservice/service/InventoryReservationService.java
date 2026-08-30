package com.portfolio.inventoryservice.service;

import com.portfolio.inventoryservice.event.InventoryResultEvent;
import com.portfolio.inventoryservice.event.OrderCreatedEvent;
import com.portfolio.inventoryservice.exception.InsufficientStockException;
import com.portfolio.inventoryservice.exception.ProductNotFoundException;
import com.portfolio.inventoryservice.persistence.entity.InventoryItemEntity;
import com.portfolio.inventoryservice.persistence.entity.InventoryReservationEntity;
import com.portfolio.inventoryservice.persistence.entity.OutboxEventEntity;
import com.portfolio.inventoryservice.persistence.entity.ProcessedEventEntity;
import com.portfolio.inventoryservice.persistence.repository.InventoryItemRepository;
import com.portfolio.inventoryservice.persistence.repository.InventoryReservationRepository;
import com.portfolio.inventoryservice.persistence.repository.OutboxEventRepository;
import com.portfolio.inventoryservice.persistence.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class InventoryReservationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InventoryReservationService.class
            );

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository
            inventoryReservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryReservationService(
            InventoryItemRepository inventoryItemRepository,
            InventoryReservationRepository
                    inventoryReservationRepository,
            ProcessedEventRepository processedEventRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryReservationRepository =
                inventoryReservationRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(OrderCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {}", event.eventId());
            return;
        }

        InventoryResultEvent resultEvent;

        try {
            reserveAll(event);

            resultEvent = createResult(
                    event.orderId(),
                    InventoryResultEvent.Result.RESERVED,
                    null
            );
        } catch (
                ProductNotFoundException
                | InsufficientStockException exception
        ) {
            resultEvent = createResult(
                    event.orderId(),
                    InventoryResultEvent.Result.REJECTED,
                    exception.getMessage()
            );
        }

        storeResult(resultEvent);

        processedEventRepository.save(
                new ProcessedEventEntity(
                        event.eventId(),
                        "OrderCreated",
                        Instant.now()
                )
        );

        log.info(
                "Inventory result for order {}: {}",
                event.orderId(),
                resultEvent.result()
        );
    }

    private void reserveAll(OrderCreatedEvent event) {
        Map<String, Integer> requestedQuantities =
                aggregateQuantities(event);

        Map<String, InventoryItemEntity> lockedItems =
                new LinkedHashMap<>();

        requestedQuantities.forEach((productId, quantity) -> {
            InventoryItemEntity item = inventoryItemRepository
                    .findByProductIdForUpdate(productId)
                    .orElseThrow(() ->
                            new ProductNotFoundException(productId)
                    );

            lockedItems.put(productId, item);
        });

        requestedQuantities.forEach((productId, quantity) -> {
            InventoryItemEntity item = lockedItems.get(productId);

            if (item.getAvailableQuantity() < quantity) {
                throw new InsufficientStockException(
                        productId,
                        quantity,
                        item.getAvailableQuantity()
                );
            }
        });

        requestedQuantities.forEach((productId, quantity) -> {
            InventoryItemEntity item = lockedItems.get(productId);

            item.reserve(quantity);

            inventoryReservationRepository.save(
                    new InventoryReservationEntity(
                            UUID.randomUUID(),
                            event.orderId(),
                            productId,
                            quantity,
                            "RESERVED",
                            Instant.now()
                    )
            );
        });
    }

    private Map<String, Integer> aggregateQuantities(
            OrderCreatedEvent event
    ) {
        Map<String, Integer> quantities = new TreeMap<>();

        event.items().forEach(item ->
                quantities.merge(
                        item.productId(),
                        item.quantity(),
                        Integer::sum
                )
        );

        return quantities;
    }

    private InventoryResultEvent createResult(
            UUID orderId,
            InventoryResultEvent.Result result,
            String reason
    ) {
        return new InventoryResultEvent(
                UUID.randomUUID(),
                orderId,
                result,
                reason,
                Instant.now(),
                1
        );
    }

    private void storeResult(InventoryResultEvent event) {
        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize inventory result",
                    exception
            );
        }

        String eventType = switch (event.result()) {
            case RESERVED -> "InventoryReserved";
            case REJECTED -> "InventoryRejected";
        };

        outboxEventRepository.save(
                new OutboxEventEntity(
                        event.eventId(),
                        "ORDER",
                        event.orderId(),
                        eventType,
                        payload,
                        event.occurredAt()
                )
        );
    }
}