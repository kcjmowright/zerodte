package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.StockPriceUpdate;
import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceService {

  private final SimpMessagingTemplate messagingTemplate;
  private final GEXPredictor predictionService;
  private final GEXService gexService;
  private final PriceService priceService;
  private final LRUCache<String> subscribedSymbols = new LRUCache<>(4);

  static class LRUCache<V> implements Iterable<V> {
    private final Map<V, V> cache;

    public LRUCache(int capacity) {
      this.cache = Collections.synchronizedMap(new LinkedHashMap<>(capacity, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<V, V> eldest) {
          return size() > capacity;
        }
      });
    }

    public void add(V key) {
      cache.put(key, key);
    }

    public void remove(V key) {
      cache.remove(key);
    }

    @Override
    public Iterator<V> iterator() {
      return cache.keySet().iterator();
    }
  }

  public void addSymbol(String symbol) {
    subscribedSymbols.add(symbol);
  }

  public void removeSymbol(String symbol) {
    subscribedSymbols.remove(symbol);
  }

  @Scheduled(cron = "*/5 */1 8-15 * * MON-FRI") // Every 5 seconds every minute between 8 and 15 Mon - Fri minute
  public void publishPriceUpdates() {
    log.info("Publishing price updates");
    subscribedSymbols.forEach(symbol -> {
      log.info("Publishing price update for {}", symbol);
      try {
        StockPriceUpdate update = fetchAndPredict(symbol);
        messagingTemplate.convertAndSend(
            "/topic/stock/" + symbol,
            update
        );
      } catch (Exception e) {
        log.error("Error updating price for {}", symbol, e);
      }
    });
  }

  private StockPriceUpdate fetchAndPredict(String symbol) {
    BigDecimal currentPrice =
        priceService.getQuote(symbol).blockOptional().map(QuoteEntity::getMark).orElse(BigDecimal.ZERO);
    List<GEXData> history = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        LocalDateTime.now().minusDays(4),
        LocalDateTime.now().plusDays(1)
    );
    Map<Integer, PricePrediction> predictions = (history != null && history.size() > 15) ?
        predictionService.predictMultiHorizon(
            history.getLast(),
            history,
            List.of(15, 30, 60)
        ) : Map.of();

    return new StockPriceUpdate(
        symbol,
        currentPrice,
        LocalDateTime.now(),
        predictions
    );
  }
}
