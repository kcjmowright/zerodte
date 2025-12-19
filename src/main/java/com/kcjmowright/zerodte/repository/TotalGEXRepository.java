package com.kcjmowright.zerodte.repository;

import com.kcjmowright.zerodte.model.entity.TotalGEXEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public interface TotalGEXRepository extends JpaRepository<TotalGEXEntity, Long> {

  @Query(value = "SELECT created FROM totalgex WHERE symbol = :symbol AND created between :start and :end ORDER BY created", nativeQuery = true)
  List<Timestamp> findTimestampsBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  default List<LocalDateTime> findBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end) {
    return findTimestampsBySymbolAndCreatedBetween(symbol, start, end).stream()
        .map(Timestamp::toLocalDateTime).toList();
  }

  TotalGEXEntity findBySymbolAndCreated(String symbol, LocalDateTime created);
}
