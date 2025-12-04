package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.TotalGEX;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainRequest;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import com.pangility.schwab.api.client.marketdata.model.expirationchain.Expiration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GammaExposureService {
  private final SchwabMarketDataApiClient marketDataClient;

  public Mono<TotalGEX> computeGammaExposure(String symbol, List<LocalDate> expirationDates, boolean suppressDetails) {
    LocalDate from;
    LocalDate to;
    if (Objects.isNull(expirationDates) || expirationDates.isEmpty()) {
      from = to = LocalDate.now();
      expirationDates = List.of(from);
    } else if (expirationDates.size() == 1) {
      from = to = expirationDates.getFirst();
    } else {
      expirationDates.sort(Comparator.naturalOrder());
      from = expirationDates.getFirst();
      to = expirationDates.getLast();
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
}
