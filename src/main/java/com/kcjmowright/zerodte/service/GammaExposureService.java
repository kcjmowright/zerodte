package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.TotalGEX;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainRequest;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GammaExposureService {
  private final SchwabMarketDataApiClient marketDataClient;

  public Mono<TotalGEX> computeGammaExposure(String symbol, LocalDate from, LocalDate to) {
    OptionChainRequest request = OptionChainRequest.builder()
        .withSymbol(symbol)
        .withFromDate(from)
        .withToDate(to)
        .build();
    return marketDataClient.fetchOptionChainToMono(request).map(r -> {
          try (Stream<OptionContract> contractStream = Stream.concat(
              r.getCallExpDateMap().values().stream().flatMap(m -> m.values().stream().flatMap(Collection::stream)),
              r.getPutExpDateMap().values().stream().flatMap(m -> m.values().stream().flatMap(Collection::stream)))) {
            return TotalGEX.fromOptionContracts(contractStream, r.getUnderlyingPrice());
          }
        });
  }

}
