package com.portfolio.inventoryservice.messaging;

import com.portfolio.inventoryservice.event.OrderCancelledEvent;
import com.portfolio.inventoryservice.event.OrderCreatedEvent;
import com.portfolio.inventoryservice.service.InventoryCancellationService;
import com.portfolio.inventoryservice.service.InventoryReservationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryReservationService reservationService;
    private final InventoryCancellationService cancellationService;

    public OrderEventConsumer(
            ObjectMapper objectMapper,
            InventoryReservationService reservationService,
            InventoryCancellationService cancellationService
    ) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
        this.cancellationService = cancellationService;
    }

    @KafkaListener(
            topics = "${inventory.kafka.order-events-topic:order-events}"
    )
    public void consume(
            String payload,
            @Header(
                    name = "eventType",
                    required = false
            )
            String eventType
    ) {
        /*
         * Events published before the header was introduced are
         * OrderCreated events, so null remains backward-compatible.
         */
        String resolvedEventType =
                eventType == null ? "OrderCreated" : eventType;

        try {
            switch (resolvedEventType) {
                case "OrderCreated" ->
                        processCreated(payload);

                case "OrderCancelled" ->
                        processCancelled(payload);

                default -> throw new IllegalArgumentException(
                        "Unsupported order event type: "
                                + resolvedEventType
                );
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid " + resolvedEventType
                            + " event payload",
                    exception
            );
        }
    }

    private void processCreated(String payload)
            throws Exception {

        OrderCreatedEvent event = objectMapper.readValue(
                payload,
                OrderCreatedEvent.class
        );

        validateVersion(event.eventVersion());
        reservationService.process(event);
    }

    private void processCancelled(String payload)
            throws Exception {

        OrderCancelledEvent event = objectMapper.readValue(
                payload,
                OrderCancelledEvent.class
        );

        validateVersion(event.eventVersion());
        cancellationService.process(event);
    }

    private void validateVersion(int version) {
        if (version != 1) {
            throw new IllegalArgumentException(
                    "Unsupported event version: " + version
            );
        }
    }
}