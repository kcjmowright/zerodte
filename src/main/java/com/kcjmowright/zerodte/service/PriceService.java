package com.kcjmowright.zerodte.service;

import com.kcjmowright.exceptions.ResourceNotAvailableException;
import com.kcjmowright.zerodte.model.IronCondorContracts;
import com.kcjmowright.zerodte.model.PriceHistoryStudy;
import com.kcjmowright.zerodte.model.PriceHistoryStudyResponse;
import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import com.kcjmowright.zerodte.repository.QuoteRepository;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainRequest;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainResponse;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import com.pangility.schwab.api.client.marketdata.model.movers.MoversRequest;
import com.pangility.schwab.api.client.marketdata.model.movers.Screener;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.FrequencyType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PeriodType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PriceHistoryRequest;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.function.Predicate.not;

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

  /**
   * Get the {@link QuoteResponse}s for the given equity symbols.
   * @param symbols equity symbols.
   * @return {@link QuoteResponse}s.
   */
  public Flux<QuoteResponse> getQuoteResponses(Collection<String> symbols) {
    return Flux.fromIterable(symbols).flatMap(symbol -> Flux.from(getQuoteResponse(symbol)));
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

  /**
   * Get the movers for a given index symbol.
   * @param indexSymbol the {@link MoversRequest.IndexSymbol}
   * @return a {@link Screener}
   */
  public Flux<Screener> callMovers(MoversRequest.IndexSymbol indexSymbol) {
    MoversRequest moversRequest = MoversRequest.builder().withIndexSymbol(indexSymbol).build();
    return marketDataClient.fetchMoversToFlux(moversRequest);
  }

  /**
   * Get the option chains for the given date range.
   * @param symbol the underlying equity symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   * @return the {@link OptionChainResponse}
   */
  public Mono<OptionChainResponse> getOptionChain(String symbol, LocalDate fromDate, LocalDate toDate) {
    var request = OptionChainRequest.builder().withFromDate(fromDate).withToDate(toDate).withSymbol(symbol).build();
    return marketDataClient.fetchOptionChainToMono(request)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }


  /**
   * Find the contracts that would make up an Iron Condor for the given underlying symbol and date range.
   * @param symbol the underlying equity symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   * @return a {@link IronCondorContracts}
   */
  public Mono<IronCondorContracts> findIronCondor(
      String symbol,
      LocalDate fromDate,
      LocalDate toDate,
      BigDecimal putLongDelta,
      BigDecimal putShortDelta,
      BigDecimal callShortDelta,
      BigDecimal callLongDelta

  ) {
    return getOptionChain(symbol, fromDate, toDate)
        .handle((optionChainResponse, sink) -> {
          if (optionChainResponse.getCallExpDateMap() == null
              || optionChainResponse.getCallExpDateMap().isEmpty()
              || optionChainResponse.getPutExpDateMap() == null
              || optionChainResponse.getPutExpDateMap().isEmpty()) {
            log.error("Option chain is empty or null.");
            sink.error(new ResourceNotAvailableException("Option chain is not currently available."));
            return;
          }

          List<OptionContract> callContracts =
              optionChainResponse.getCallExpDateMap().values().iterator().next().values().stream()
                  .flatMap(Collection::stream).toList();
          List<OptionContract> putContracts =
              optionChainResponse.getPutExpDateMap().values().iterator().next().values().stream()
                  .flatMap(Collection::stream).toList();

          Optional<OptionContract> optionalShortPut =
              findOption(putContracts, putShortDelta, OptionContract.PutCall.PUT);
          Optional<OptionContract> optionalLongPut =
              findOption(putContracts, putLongDelta, OptionContract.PutCall.PUT);
          Optional<OptionContract> optionalShortCall =
              findOption(callContracts, callShortDelta, OptionContract.PutCall.CALL);
          Optional<OptionContract> optionalLongCall =
              findOption(callContracts, callLongDelta, OptionContract.PutCall.CALL);

          if (optionalShortPut.isEmpty()
              || optionalLongPut.isEmpty()
              || optionalShortCall.isEmpty()
              || optionalLongCall.isEmpty()) {
            log.error("Could not find required options.");
            sink.error(new ResourceNotAvailableException("Could not find required options"));
            return;
          }

          OptionContract shortPut = optionalShortPut.get();
          OptionContract longPut = optionalLongPut.get();
          OptionContract shortCall = optionalShortCall.get();
          OptionContract longCall = optionalLongCall.get();
          sink.next(new IronCondorContracts(longCall, shortCall, longPut, shortPut));
        });
  }


  /**
   * Find option contracts with the given delta and option type.
   * @param options a list of option contracts.
   * @param delta the target delta.
   * @param putCall the type of contract, PUT or CALL.
   * @return the option contract that most closely matches the given parameters.
   */
  public Optional<OptionContract> findOption(
      List<OptionContract> options,
      BigDecimal delta,
      OptionContract.PutCall putCall) {

    return options.stream()
        .filter(option -> option.getPutCall() == putCall)
        .filter(not(option -> Objects.equals(option.getDelta(), BigDecimal.ZERO)))
        .min(Comparator.comparing(option -> option.getDelta().subtract(delta).abs()));
  }

}
