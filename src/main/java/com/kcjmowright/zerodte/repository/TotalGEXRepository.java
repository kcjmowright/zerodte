package com.kcjmowright.zerodte.repository;

import com.kcjmowright.zerodte.model.entity.TotalGEXEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TotalGEXRepository extends JpaRepository<TotalGEXEntity, Long> {

  @Query(value = "SELECT created FROM totalgex WHERE symbol = :symbol AND created between :start and :end", nativeQuery = true)
  List<LocalDateTime> findBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  TotalGEXEntity findBySymbolAndCreated(String symbol, LocalDateTime created);
}
