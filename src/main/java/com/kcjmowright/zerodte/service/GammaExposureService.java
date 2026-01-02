package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.OptionContractGEX;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.entity.TotalGEXEntity;
import com.kcjmowright.zerodte.repository.TotalGEXRepository;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainRequest;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import com.pangility.schwab.api.client.marketdata.model.expirationchain.Expiration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GammaExposureService {
  private final SchwabMarketDataApiClient marketDataClient;
  private final TotalGEXRepository totalGEXRepository;

  public Mono<TotalGEX> computeGammaExposure(String symbol, List<LocalDate> expirationDates, boolean suppressDetails) {
    LocalDate from;
    LocalDate to;
    if (Objects.isNull(expirationDates) || expirationDates.isEmpty()) {
      from = LocalDate.now();
      to = LocalDate.now().plusDays(1);
      expirationDates = List.of(from);
    } else if (expirationDates.size() == 1) {
      from = expirationDates.getFirst();
      to = from.plusDays(1);
    } else {
      expirationDates.sort(Comparator.naturalOrder());
      from = expirationDates.getFirst();
      to = expirationDates.getLast().plusDays(1);
    }
    final OptionChainRequest request = OptionChainRequest.builder()
        .withSymbol(symbol)
        .withFromDate(from)
        .withToDate(to)
        .withIncludeQuotes(true)
        .build();
    final Set<LocalDate> expirationDatesSet = new HashSet<>(expirationDates);
    return marketDataClient.fetchOptionChainToMono(request)
        .map(r -> {
          try (Stream<OptionContract> contractStream = Stream.concat(
              r.getCallExpDateMap().values().stream()
                  .flatMap(m -> m.values().stream().flatMap(Collection::stream)),
              r.getPutExpDateMap().values().stream()
                  .flatMap(m -> m.values().stream().flatMap(Collection::stream)))
              .filter(c -> expirationDatesSet.contains(c.getExpirationDate().toLocalDate()))) {
            return TotalGEX.fromOptionContracts(contractStream, r.getUnderlyingPrice(), suppressDetails);
          }
        });
  }

  public Flux<LocalDate> fetchExpirationDates(String symbol) {
    return marketDataClient.fetchExpirationChainToMono(symbol).flatMapMany(res ->
        Flux.fromStream(res.getExpirationList().stream().map(Expiration::getExpirationDate)));
  }

  @Scheduled(cron = "0 */1 8-15 * * MON-FRI")
  public void captureGammaExposure() {
    List.of("QQQ", "SPY", "$SPX", "IWM").forEach(symbol ->
        computeGammaExposure(symbol, null, true).flatMap(totalGEX -> {
          TotalGEXEntity entity = new TotalGEXEntity();
          entity.setData(totalGEX);
          entity.setSymbol(symbol);
          entity.setCreated(LocalDateTime.now());
          totalGEXRepository.save(entity);
          return Mono.just(entity);
        })
        .subscribe(
            savedEntity -> log.info("Successfully saved TotalGEX for symbol {} with id: {}", symbol, savedEntity.getId()),
            error -> log.error("Error saving data for symbol {}", symbol, error),
            () -> log.info("Completed data fetch and save for symbol {}", symbol)
        ));
  }

  public Flux<LocalDateTime> findTotalGEXCaptureDateTimes(String symbol, LocalDateTime start, LocalDateTime end) {
    return Flux.fromStream(totalGEXRepository.findBySymbolAndCreatedBetween(symbol, start, end).stream());
  }

  public Flux<TotalGEX> findTotalGEXBySymbolBetween(String symbol, LocalDateTime start, LocalDateTime end) {
    return Flux.fromStream(totalGEXRepository.getTotalGEXBySymbolBetween(symbol, start, end).stream());
  }

  public Mono<TotalGEX> getTotalGEXCapture(String symbol, LocalDateTime created) {
    return Mono.fromCallable(() -> totalGEXRepository.findBySymbolAndCreated(symbol, created).getData())
        .map(totalGEX -> {
          TreeMap<BigDecimal, OptionContractGEX> sortedMap = new TreeMap<>(Comparator.reverseOrder());
          sortedMap.putAll(totalGEX.getGexPerStrike());
          totalGEX.setGexPerStrike(sortedMap);
          return totalGEX;
        });
  }

  public TotalGEX getLatestBySymbol(String symbol) {
    return totalGEXRepository.getLatestBySymbol(symbol);
  }

  public List<TotalGEX> getMostRecentBySymbol(String symbol, int limit) {
    return totalGEXRepository.getMostRecentBySymbol(symbol, limit);
  }
}
