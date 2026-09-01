package com.portfolio.inventoryservice.service;

import com.portfolio.inventoryservice.event.OrderCancelledEvent;
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
import java.util.List;

@Service
public class InventoryCancellationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InventoryCancellationService.class
            );

    private final InventoryReservationRepository
            reservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryCancellationService(
            InventoryReservationRepository reservationRepository,
            InventoryItemRepository inventoryItemRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(OrderCancelledEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info(
                    "Skipping duplicate cancellation event {}",
                    event.eventId()
            );
            return;
        }

        List<InventoryReservationEntity> reservations =
                reservationRepository
                        .findAllByOrderIdAndStatusOrderByProductIdAsc(
                                event.orderId(),
                                "RESERVED"
                        );

        for (InventoryReservationEntity reservation :
                reservations) {

            InventoryItemEntity inventoryItem =
                    inventoryItemRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Reserved product no longer exists: "
                                                    + reservation
                                                    .getProductId()
                                    )
                            );

            inventoryItem.release(reservation.getQuantity());
            reservation.release();
        }

        processedEventRepository.save(
                new ProcessedEventEntity(
                        event.eventId(),
                        "OrderCancelled",
                        Instant.now()
                )
        );

        log.info(
                "Released {} inventory reservations for order {}",
                reservations.size(),
                event.orderId()
        );
    }
}