package com.portfolio.inventoryservice.persistence.repository;

import com.portfolio.inventoryservice.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEventEntity, UUID> {
}