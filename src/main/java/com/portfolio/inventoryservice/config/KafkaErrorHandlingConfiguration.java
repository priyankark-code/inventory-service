package com.portfolio.inventoryservice.config;

import com.portfolio.inventoryservice.exception.InsufficientStockException;
import com.portfolio.inventoryservice.exception.ProductNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlingConfiguration {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        record.partition()
                                )
                );

        recoverer.setFailIfSendResultIsError(true);

        FixedBackOff backOff = new FixedBackOff(
                1_000L,
                2L
        );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        backOff
                );

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                ProductNotFoundException.class,
                InsufficientStockException.class
        );

        return errorHandler;
    }
}