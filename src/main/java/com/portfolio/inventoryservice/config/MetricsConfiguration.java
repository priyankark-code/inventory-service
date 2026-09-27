package com.portfolio.inventoryservice.config;

import com.portfolio.inventoryservice.persistence.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterBinder pendingOutboxEvents(
            OutboxEventRepository repository
    ) {
        return registry -> Gauge.builder(
                        "outbox.events.pending",
                        repository,
                        OutboxEventRepository::
                                countByPublishedAtIsNull
                )
                .description(
                        "Number of outbox events awaiting publication"
                )
                .register(registry);
    }
}