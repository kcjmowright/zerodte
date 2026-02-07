package com.kcjmowright.zerodte.repository;

import com.kcjmowright.zerodte.model.GEXDataProjection;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.entity.TotalGEXEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TotalGEXRepository extends JpaRepository<TotalGEXEntity, Long> {

  @Query(value = "SELECT tg.created FROM TotalGEXEntity tg WHERE tg.symbol = :symbol AND tg.created between :start and :end ORDER BY tg.created")
  List<LocalDateTime> findCreatedBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  List<TotalGEXEntity> getTotalGEXEntityBySymbolAndCreatedBetween(String symbol, LocalDateTime start, LocalDateTime end);

  default List<TotalGEX> getTotalGEXBySymbolBetween(String symbol, LocalDateTime start, LocalDateTime end) {
    return getTotalGEXEntityBySymbolAndCreatedBetween(symbol, start, end).stream().map(TotalGEXEntity::getData).toList();
  }

  TotalGEXEntity findBySymbolAndCreated(String symbol, LocalDateTime created);

  TotalGEXEntity getTopBySymbolOrderByCreatedDesc(String symbol);

  default TotalGEX getLatestBySymbol(String symbol) {
    TotalGEXEntity totalGEXEntity = getTopBySymbolOrderByCreatedDesc(symbol);
    return totalGEXEntity == null ? null : totalGEXEntity.getData();
  }

  @Query("SELECT data FROM TotalGEXEntity WHERE symbol = :symbol ORDER BY created DESC LIMIT :limit")
  List<TotalGEX> getMostRecentBySymbol(String symbol, int limit);

  @Query(value = """
    SELECT
        a.created as created,
        a.symbol as symbol,
        a.data as totalGEX,
        b.mark as vix,
        q.open as open,
        q.close as close,
        q.low as low,
        q.high as high
    FROM totalgex a
        LEFT JOIN quote b ON a.created = b.created
        LEFT OUTER JOIN quote q ON a.created = q.created
    WHERE a.symbol = :symbol
        AND q.symbol = :symbol
        AND b.symbol = '$VIX'
        AND a.created BETWEEN :start AND :end
    ORDER BY created""", nativeQuery = true)
  List<GEXDataProjection> getGEXDataBySymbolBetweenStartAndEnd(String symbol, LocalDateTime start, LocalDateTime end);
}
