package com.portfolio.inventoryservice;

import com.portfolio.inventoryservice.event.OrderCancelledEvent;
import com.portfolio.inventoryservice.event.OrderCreatedEvent;
import com.portfolio.inventoryservice.persistence.entity.InventoryItemEntity;
import com.portfolio.inventoryservice.persistence.entity.InventoryReservationEntity;
import com.portfolio.inventoryservice.persistence.entity.OutboxEventEntity;
import com.portfolio.inventoryservice.persistence.repository.InventoryItemRepository;
import com.portfolio.inventoryservice.persistence.repository.InventoryReservationRepository;
import com.portfolio.inventoryservice.persistence.repository.OutboxEventRepository;
import com.portfolio.inventoryservice.persistence.repository.ProcessedEventRepository;
import com.portfolio.inventoryservice.service.InventoryCancellationService;
import com.portfolio.inventoryservice.service.InventoryReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "inventory.outbox.publisher.enabled=false"
})
@Testcontainers
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_CLASS
)
@Transactional
class InventoryProcessingIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(
                    "apache/kafka-native:3.8.0"
            );

    @Autowired
    private InventoryReservationService reservationService;

    @Autowired
    private InventoryCancellationService cancellationService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void duplicateOrderCreatedShouldReserveOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent event = orderCreatedEvent(
                eventId,
                orderId,
                2
        );

        reservationService.process(event);
        reservationService.process(event);

        InventoryItemEntity inventory =
                inventoryItemRepository
                        .findById("product-001")
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(98);
        assertThat(inventory.getReservedQuantity())
                .isEqualTo(2);

        assertThat(
                reservationRepository.findAllByOrderId(orderId)
        ).hasSize(1);

        assertThat(processedEventRepository.existsById(eventId))
                .isTrue();

        assertThat(outboxEventRepository.count())
                .isEqualTo(1);
    }

    @Test
    void insufficientStockShouldRejectWithoutChangingInventory()
            throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        reservationService.process(
                orderCreatedEvent(
                        eventId,
                        orderId,
                        10_000
                )
        );

        InventoryItemEntity inventory =
                inventoryItemRepository
                        .findById("product-001")
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(100);
        assertThat(inventory.getReservedQuantity())
                .isZero();

        assertThat(
                reservationRepository.findAllByOrderId(orderId)
        ).isEmpty();

        assertThat(processedEventRepository.existsById(eventId))
                .isTrue();

        List<OutboxEventEntity> events =
                outboxEventRepository.findAll();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType())
                .isEqualTo("InventoryRejected");

        JsonNode payload = objectMapper.readTree(
                events.getFirst().getPayload()
        );

        assertThat(payload.get("orderId").asText())
                .isEqualTo(orderId.toString());

        assertThat(payload.get("result").asText())
                .isEqualTo("REJECTED");

        assertThat(payload.get("reason").asText())
                .contains("Insufficient stock");
    }

    @Test
    void cancellationShouldReleaseInventoryExactlyOnce() {
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent createdEvent = orderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                3
        );

        reservationService.process(createdEvent);

        OrderCancelledEvent cancelledEvent =
                new OrderCancelledEvent(
                        UUID.randomUUID(),
                        orderId,
                        Instant.now(),
                        1
                );

        cancellationService.process(cancelledEvent);
        cancellationService.process(cancelledEvent);

        InventoryItemEntity inventory =
                inventoryItemRepository
                        .findById("product-001")
                        .orElseThrow();

        assertThat(inventory.getAvailableQuantity())
                .isEqualTo(100);
        assertThat(inventory.getReservedQuantity())
                .isZero();

        List<InventoryReservationEntity> reservations =
                reservationRepository.findAllByOrderId(orderId);

        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus())
                .isEqualTo("RELEASED");

        assertThat(
                processedEventRepository.existsById(
                        createdEvent.eventId()
                )
        ).isTrue();

        assertThat(
                processedEventRepository.existsById(
                        cancelledEvent.eventId()
                )
        ).isTrue();
    }

    private OrderCreatedEvent orderCreatedEvent(
            UUID eventId,
            UUID orderId,
            int quantity
    ) {
        return new OrderCreatedEvent(
                eventId,
                orderId,
                "integration-test-customer",
                List.of(
                        new OrderCreatedEvent.Item(
                                "product-001",
                                quantity,
                                new BigDecimal("49.99")
                        )
                ),
                new BigDecimal("49.99")
                        .multiply(BigDecimal.valueOf(quantity)),
                Instant.now(),
                1
        );
    }
}