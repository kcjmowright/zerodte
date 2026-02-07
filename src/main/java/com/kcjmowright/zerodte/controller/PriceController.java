package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.IronCondorContracts;
import com.kcjmowright.zerodte.model.PriceHistoryStudyResponse;
import com.kcjmowright.zerodte.service.PriceService;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainResponse;
import com.pangility.schwab.api.client.marketdata.model.movers.MoversRequest;
import com.pangility.schwab.api.client.marketdata.model.movers.Screener;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.FrequencyType;
import com.pangility.schwab.api.client.marketdata.model.pricehistory.PeriodType;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class PriceController {

  private final PriceService priceService;

  @GetMapping("/price")
  public Mono<QuoteResponse> getPrice(@RequestParam String symbol) {
    return priceService.getQuoteResponse(symbol);
  }

  @GetMapping("/price-history/{symbol}")
  public Mono<PriceHistoryStudyResponse> getPriceHistoryStudy(
      @PathVariable("symbol") String symbol,
      @RequestParam("start") LocalDate start,
      @RequestParam("end") LocalDate end,
      @RequestParam(name = "periodType", defaultValue = "day") PeriodType periodType,
      @RequestParam(name = "period", defaultValue = "1") Integer period,
      @RequestParam(name = "frequencyType", defaultValue = "minute") FrequencyType frequencyType,
      @RequestParam(name = "frequency", defaultValue = "1") Integer frequency,
      @RequestParam(name = "load", defaultValue = "false") Boolean load) {

    return priceService.getPriceHistoryStudy(
        symbol,
        start,
        end,
        frequencyType,
        frequency,
        periodType,
        period,
        List.of(),
        load
    );
  }

  /**
   *
   * @param indexSymbol
   *                    $DJI,
   *                    $COMPX,
   *                    $SPX,
   *                    NYSE,
   *                    NASDAQ,
   *                    OTCBB,
   *                    INDEX_ALL,
   *                    EQUITY_ALL,
   *                    OPTION_ALL,
   *                    OPTION_PUT,
   *                    OPTION_CALL;
   * @return Screener
   */
  @GetMapping("/movers/{indexSymbol}")
  public Flux<Screener> getMovers(@PathVariable String indexSymbol) {
    return priceService.callMovers(MoversRequest.IndexSymbol.valueOf(indexSymbol));
  }

  @GetMapping("/option/chain/{symbol}")
  public Mono<OptionChainResponse> getOptionChain(
      @PathVariable String symbol,
      @RequestParam(value = "fromDate") LocalDate fromDate,
      @RequestParam(value = "toDate") LocalDate toDate) {
    return priceService.getOptionChain(symbol, fromDate, toDate);
  }

  @GetMapping("/option/zero-dte-iron-condor")
  public Mono<IronCondorContracts> getZeroDTEIronCondor(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "putLongDelta", required = false, defaultValue = "-10") BigDecimal putLongDelta,
      @RequestParam(value = "putShortDelta", required = false, defaultValue = "-40") BigDecimal putShortDelta,
      @RequestParam(value = "callShortDelta", required = false, defaultValue = "40") BigDecimal callShortDelta,
      @RequestParam(value = "callLongDelta", required = false, defaultValue = "10") BigDecimal callLongDelta) {
    final LocalDate theDate = LocalDate.now();
    return priceService.findIronCondor(symbol, theDate, theDate, putLongDelta, putShortDelta, callShortDelta, callLongDelta);
  }

  @GetMapping("/option/iron-condor")
  public Mono<IronCondorContracts> getIronCondor(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "dte", required = false) LocalDate dte,
      @RequestParam(value = "putLongDelta", required = false, defaultValue = "-10") BigDecimal putLongDelta,
      @RequestParam(value = "putShortDelta", required = false, defaultValue = "-40") BigDecimal putShortDelta,
      @RequestParam(value = "callShortDelta", required = false, defaultValue = "40") BigDecimal callShortDelta,
      @RequestParam(value = "callLongDelta", required = false, defaultValue = "10") BigDecimal callLongDelta) {
    final LocalDate theDate = dte == null ? LocalDate.now() : dte;
    return priceService.findIronCondor(symbol, theDate, theDate, putLongDelta, putShortDelta, callShortDelta, callLongDelta);
  }
}
