package com.portfolio.inventoryservice.outbox;

import com.portfolio.inventoryservice.persistence.entity.OutboxEventEntity;
import com.portfolio.inventoryservice.persistence.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "inventory.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxEventPublisher.class);

    private static final String TOPIC = "inventory-events";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventStateService stateService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxEventStateService stateService,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.stateService = stateService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(
            fixedDelayString =
                    "${inventory.outbox.publisher.delay-ms:1000}"
    )
    public void publishPendingEvents() {
        List<OutboxEventEntity> events =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).get(10, TimeUnit.SECONDS);

            stateService.markPublished(
                    event.getId(),
                    Instant.now()
            );

            log.info(
                    "Published inventory result {} for order {}",
                    event.getId(),
                    event.getAggregateId()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to publish inventory result {}",
                    event.getId(),
                    exception
            );
        }
    }
}