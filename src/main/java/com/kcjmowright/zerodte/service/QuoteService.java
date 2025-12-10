package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.QuoteStudy;
import com.kcjmowright.zerodte.model.QuoteStudyResponse;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.FrequencyType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PeriodType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PriceHistoryRequest;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PriceHistoryResponse;
import com.pangility.schwab.api.client.marketdata.model.quotes.Quote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class QuoteService {
  private final SchwabMarketDataApiClient marketDataClient;

  public Mono<QuoteStudyResponse> getQuoteStudy(
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
          QuoteStudyResponse quotesStudyResponse = new QuoteStudyResponse();
          quotesStudyResponse.setSymbol(symbol);
          var l = response.getCandles().stream().map(c -> {
            QuoteStudy quoteStudy = new QuoteStudy();
            quoteStudy.setCandle(c);
            return quoteStudy;
          }).toList();
          quotesStudyResponse.setQuoteStudies(l);
          sink.next(quotesStudyResponse);
        });
  }

}
