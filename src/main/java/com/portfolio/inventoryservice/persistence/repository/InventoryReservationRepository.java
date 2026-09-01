package com.portfolio.inventoryservice.persistence.repository;

import com.portfolio.inventoryservice.persistence.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservationEntity, UUID> {

    long countByOrderId(UUID orderId);

    List<InventoryReservationEntity> findAllByOrderIdAndStatusOrderByProductIdAsc(UUID orderId, String status);
}