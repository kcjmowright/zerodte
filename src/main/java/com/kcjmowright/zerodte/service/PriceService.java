package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.PriceHistoryStudy;
import com.kcjmowright.zerodte.model.PriceHistoryStudyResponse;
import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import com.kcjmowright.zerodte.repository.QuoteRepository;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.FrequencyType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PeriodType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PriceHistoryRequest;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {
  private final SchwabMarketDataApiClient marketDataClient;
  private final QuoteRepository quoteRepository;

  public Mono<QuoteResponse> getQuoteResponse(String symbol) {
    return marketDataClient.fetchQuoteToMono(symbol)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
  }

  public Mono<QuoteEntity> getQuote(String symbol) {
    return getQuoteResponse(symbol).map(this::getQuote);
  }

  public Mono<PriceHistoryStudyResponse> getPriceHistoryStudy(
      String symbol,
      LocalDate start,
      LocalDate end,
      FrequencyType frequencyType,
      Integer frequency,
      PeriodType periodType,
      Integer period,
      Collection<String> studies) {
    PriceHistoryRequest req = PriceHistoryRequest.builder()
        .withStartDate(start)
        .withEndDate(end)
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

  @Scheduled(cron = "0 */1 8-15 * * MON-FRI")
  public void captureVolatility() {
    marketDataClient.fetchQuoteToMono("$VIX")
        .flatMap(response -> Mono.just(quoteRepository.save(getQuote(response))))
        .subscribe(
            entity -> log.info("Successfully saved VIX {}", entity),
            error -> log.error("Error saving VIX data {}", error.getMessage()),
            () -> log.info("Completed fetching VIX data"));
  }

  /**
   * Transform
   * @param quoteResponse the {@link QuoteResponse}.
   * @return {@link QuoteEntity}
   */
  private QuoteEntity getQuote(QuoteResponse quoteResponse) {
    if (quoteResponse == null) {
      return null;
    }
    QuoteEntity quote = new QuoteEntity();
    quote.setSymbol(quoteResponse.getSymbol());
    switch(quoteResponse) {
      case QuoteResponse.OptionResponse or -> {
        quote.setMark(or.getQuote().getMark());
        quote.setClose(or.getQuote().getClosePrice());
        quote.setHigh(or.getQuote().getHighPrice());
        quote.setLow(or.getQuote().getLowPrice());
        quote.setOpen(or.getQuote().getOpenPrice());
        quote.setCreated(LocalDateTime.ofInstant(
            Instant.ofEpochMilli(or.getQuote().getQuoteTime()),
            ZoneId.of("UTC")
        ));
      }
      case QuoteResponse.EquityResponse er -> {
        quote.setMark(er.getQuote().getMark());
        quote.setClose(er.getQuote().getClosePrice());
        quote.setHigh(er.getQuote().getHighPrice());
        quote.setLow(er.getQuote().getLowPrice());
        quote.setOpen(er.getQuote().getOpenPrice());
        quote.setCreated(LocalDateTime.ofInstant(
            Instant.ofEpochMilli(er.getQuote().getQuoteTime()),
            ZoneId.of("UTC")
        ));
      }
      case QuoteResponse.IndexResponse ir -> {
        quote.setMark(ir.getQuote().getLastPrice());
        quote.setClose(ir.getQuote().getClosePrice());
        quote.setHigh(ir.getQuote().getHighPrice());
        quote.setLow(ir.getQuote().getLowPrice());
        quote.setOpen(ir.getQuote().getOpenPrice());
        quote.setCreated(LocalDateTime.ofInstant(
            Instant.ofEpochMilli(ir.getQuote().getTradeTime()),
            ZoneId.of("UTC")
        ));
      }
      default -> {
      }
    }
    return quote;
  }

}
