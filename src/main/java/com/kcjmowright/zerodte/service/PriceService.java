package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.PriceHistoryStudy;
import com.kcjmowright.zerodte.model.PriceHistoryStudyResponse;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.FrequencyType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PeriodType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PriceHistoryRequest;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.LocalDate;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class PriceService {
  private final SchwabMarketDataApiClient marketDataClient;

  public Mono<QuoteResponse> getPrice(String symbol) {
    return marketDataClient.fetchQuoteToMono(symbol)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }

  public Mono<PriceHistoryStudyResponse> getPriceHistoryStudy(
      String symbol,
      LocalDate startDate,
      LocalDate endDate,
      FrequencyType frequencyType,
      Integer frequency,
      PeriodType periodType,
      Integer period,
      Collection<String> studies) {
    PriceHistoryRequest req = PriceHistoryRequest.builder()
        .withStartDate(startDate)
        .withEndDate(endDate)
        .withSymbol(symbol)
        .withFrequencyType(frequencyType)
        .withFrequency(frequency)
        .withPeriodType(periodType)
        .withPeriod(period)
        .build();

    return marketDataClient.fetchPriceHistoryToMono(req)
        .handle((response, sink) -> {
          PriceHistoryStudyResponse quotesStudyResponse = new PriceHistoryStudyResponse();
          quotesStudyResponse.setSymbol(symbol);
          var l = response.getCandles().stream().map(c -> {
            PriceHistoryStudy priceHistoryStudy = new PriceHistoryStudy();
            priceHistoryStudy.setCandle(c);
            return priceHistoryStudy;
          }).toList();
          quotesStudyResponse.setPriceHistoryStudies(l);
          sink.next(quotesStudyResponse);
        });
  }

}
