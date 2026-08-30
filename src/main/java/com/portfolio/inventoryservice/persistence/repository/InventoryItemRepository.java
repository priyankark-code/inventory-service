package com.portfolio.inventoryservice.persistence.repository;

import com.portfolio.inventoryservice.persistence.entity.InventoryItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItemEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT item
            FROM InventoryItemEntity item
            WHERE item.productId = :productId
            """)
    Optional<InventoryItemEntity> findByProductIdForUpdate(
            @Param("productId") String productId
    );
}