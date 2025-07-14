package com.kcjmowright.zerodte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kcjmowright.zerodte.model.PositionEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PositionRepository extends JpaRepository<PositionEntity, Long> {

    List<PositionEntity> findByClosedNotNullAndCreatedIsAfter(LocalDateTime created);
    List<PositionEntity> findByClosedIsNull();
    List<PositionEntity> findBySymbolIn(Collection<String> symbols);
}
