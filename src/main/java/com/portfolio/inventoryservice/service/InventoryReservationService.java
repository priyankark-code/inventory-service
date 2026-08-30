package com.portfolio.inventoryservice.service;

import com.portfolio.inventoryservice.event.OrderCreatedEvent;
import com.portfolio.inventoryservice.exception.ProductNotFoundException;
import com.portfolio.inventoryservice.persistence.entity.InventoryItemEntity;
import com.portfolio.inventoryservice.persistence.entity.InventoryReservationEntity;
import com.portfolio.inventoryservice.persistence.entity.ProcessedEventEntity;
import com.portfolio.inventoryservice.persistence.repository.InventoryItemRepository;
import com.portfolio.inventoryservice.persistence.repository.InventoryReservationRepository;
import com.portfolio.inventoryservice.persistence.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
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

    public InventoryReservationService(
            InventoryItemRepository inventoryItemRepository,
            InventoryReservationRepository
                    inventoryReservationRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryReservationRepository =
                inventoryReservationRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(OrderCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate event {}", event.eventId());
            return;
        }

        event.items().stream()
                .sorted(Comparator.comparing(
                        OrderCreatedEvent.Item::productId
                ))
                .forEach(item -> reserve(event.orderId(), item));

        processedEventRepository.save(
                new ProcessedEventEntity(
                        event.eventId(),
                        "OrderCreated",
                        Instant.now()
                )
        );

        log.info("Reserved inventory for order {} from event {}", event.orderId(), event.eventId());
    }

    private void reserve(
            UUID orderId,
            OrderCreatedEvent.Item requestedItem
    ) {
        InventoryItemEntity inventoryItem =
                inventoryItemRepository
                        .findByProductIdForUpdate(
                                requestedItem.productId()
                        )
                        .orElseThrow(() ->
                                new ProductNotFoundException(
                                        requestedItem.productId()
                                )
                        );

        inventoryItem.reserve(requestedItem.quantity());

        inventoryReservationRepository.save(
                new InventoryReservationEntity(
                        UUID.randomUUID(),
                        orderId,
                        requestedItem.productId(),
                        requestedItem.quantity(),
                        "RESERVED",
                        Instant.now()
                )
        );
    }
}