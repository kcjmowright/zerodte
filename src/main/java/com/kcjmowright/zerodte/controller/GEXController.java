package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.BackTestRequest;
import com.kcjmowright.zerodte.model.BackTestResult;
import com.kcjmowright.zerodte.model.PredictionRequest;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.WalkForwardRequest;
import com.kcjmowright.zerodte.service.GEXBackTester;
import com.kcjmowright.zerodte.service.GEXPricePredictor;
import com.kcjmowright.zerodte.service.GammaExposureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/gex")
public class GEXController {

  private final GammaExposureService gammaExposureService;
  private final GEXPricePredictor predictor;
  private final GEXBackTester backtester;

  @GetMapping("/{symbol}")
  public Mono<TotalGEX> getGEX(
      @PathVariable String symbol,
      @RequestParam(value = "expDate", required = false) List<LocalDate> expirationDates,
      @RequestParam(value = "suppressDetails", required = false) @DefaultValue("true") boolean suppressDetails) {
    return gammaExposureService.computeGammaExposure(symbol, expirationDates, suppressDetails);
  }

  @GetMapping("/expirations/{symbol}")
  public Flux<LocalDate> getGEX(@PathVariable String symbol) {
    return gammaExposureService.fetchExpirationDates(symbol);
  }

  @GetMapping("/history/datetimes/{symbol}")
  public Flux<LocalDateTime> getHistoryDateTimes(
      @PathVariable String symbol,
      @RequestParam(value = "start", required = false) LocalDateTime start,
      @RequestParam(value = "end", required = false) LocalDateTime end) {
    return gammaExposureService.findTotalGEXCaptureDateTimes(
        symbol,
        start == null ? LocalDateTime.now().withHour(0).withMinute(0) : start,
        end == null ? LocalDateTime.now().withHour(23).withMinute(59) : end
    );
  }

  @GetMapping("/history/{symbol}")
  public Mono<TotalGEX> getTotalGEXHistory(
      @PathVariable String symbol,
      @RequestParam(value = "dateTime", required = false) LocalDateTime dateTime) {
    return gammaExposureService.getTotalGEXCapture(symbol, dateTime == null ? LocalDateTime.now() : dateTime);
  }

  @GetMapping("/history/latest/{symbol}")
  public Mono<TotalGEX> getLatestSnapshot(@PathVariable("symbol") String symbol) {
    return Mono.just(gammaExposureService.getLatestBySymbol(symbol));
  }

  @PostMapping("/predict")
  public Mono<PricePrediction> predictPrice(@RequestBody PredictionRequest request) {
    TotalGEX current = gammaExposureService.getLatestBySymbol(request.getSymbol());
    List<TotalGEX> history = gammaExposureService.getMostRecentBySymbol(request.getSymbol(), 60);
    PricePrediction prediction = predictor.predict(current, history, request.getMinutesAhead());
    return Mono.just(prediction);
  }

  @PostMapping("/backtest")
  public Mono<BackTestResult> runBacktest(@RequestBody BackTestRequest request) {
    Mono<List<TotalGEX>> data = gammaExposureService.findTotalGEXBySymbolBetween(
      request.getSymbol(),
      request.getStartDate(),
      request.getEndDate()
    ).collectList();
    BackTestResult result = backtester.runBacktest(
        data.block(),
        request.getPredictionHorizon(),
        request.getMinHistorySize()
    );
    return Mono.just(result);
  }

  @PostMapping("/backtest/walk-forward")
  public Mono<Map<String, Object>> walkForwardTest(@RequestBody WalkForwardRequest request) {
    Mono<List<TotalGEX>> data = gammaExposureService.findTotalGEXBySymbolBetween(
        request.getSymbol(),
        request.getStartDate(),
        request.getEndDate()
    ).collectList();
    Map<String, Object> results = backtester.walkForwardOptimization(
        data.block(),
        request.getTrainingWindow(),
        request.getTestingWindow()
    );
    return Mono.just(results);
  }

}
