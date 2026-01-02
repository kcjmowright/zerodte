package com.kcjmowright.zerodte.repository;

import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.entity.TotalGEXEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface TotalGEXRepository extends JpaRepository<TotalGEXEntity, Long> {

  @Query(value = "SELECT created FROM totalgex WHERE symbol = :symbol AND created between :start and :end ORDER BY created", nativeQuery = true)
  List<Timestamp> findTimestampsBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  default List<LocalDateTime> findBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end) {
    return findTimestampsBySymbolAndCreatedBetween(symbol, start, end).stream()
        .map(Timestamp::toLocalDateTime).toList();
  }

  Stream<TotalGEXEntity> getTotalGEXEntityBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  default List<TotalGEX> getTotalGEXBySymbolBetween(String symbol, LocalDateTime start, LocalDateTime end) {
    return getTotalGEXEntityBySymbolAndCreatedBetween(symbol, start, end).map(TotalGEXEntity::getData).toList();
  }

  TotalGEXEntity findBySymbolAndCreated(String symbol, LocalDateTime created);

  TotalGEXEntity getTopBySymbolOrderByCreatedDesc(String symbol);

  default TotalGEX getLatestBySymbol(String symbol) {
    return getTopBySymbolOrderByCreatedDesc(symbol).getData();
  }

  @Query("SELECT data FROM TotalGEXEntity WHERE symbol = :symbol ORDER BY created DESC LIMIT :limit")
  List<TotalGEX> getMostRecentBySymbol(String symbol, int limit);
}
