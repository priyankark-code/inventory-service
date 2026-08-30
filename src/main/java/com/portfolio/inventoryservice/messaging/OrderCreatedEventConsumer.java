package com.portfolio.inventoryservice.messaging;

import com.portfolio.inventoryservice.event.OrderCreatedEvent;
import com.portfolio.inventoryservice.service.InventoryReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderCreatedEventConsumer.class
            );

    private final ObjectMapper objectMapper;
    private final InventoryReservationService reservationService;

    public OrderCreatedEventConsumer(
            ObjectMapper objectMapper,
            InventoryReservationService reservationService
    ) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
    }

    @KafkaListener(
            topics = "${inventory.kafka.order-events-topic:order-events}"
    )
    public void consume(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    payload,
                    OrderCreatedEvent.class
            );

            if (event.eventVersion() != 1) {
                throw new IllegalArgumentException(
                        "Unsupported OrderCreated event version: "
                                + event.eventVersion()
                );
            }

            reservationService.process(event);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "Could not deserialize OrderCreated event",
                    exception
            );

            throw new IllegalArgumentException(
                    "Invalid OrderCreated event payload",
                    exception
            );
        }
    }
}
