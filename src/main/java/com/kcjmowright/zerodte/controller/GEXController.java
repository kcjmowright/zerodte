package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.service.GammaExposureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class GEXController {

  private final GammaExposureService gammaExposureService;

  @GetMapping("/gex/{symbol}")
  public Mono<TotalGEX> getGEX(
      @PathVariable String symbol,
      @RequestParam(value = "expDate", required = false) List<LocalDate> expirationDates,
      @RequestParam(value = "suppressDetails", required = false) @DefaultValue("true") boolean suppressDetails) {
    return gammaExposureService.computeGammaExposure(symbol, expirationDates, suppressDetails);
  }

  @GetMapping("/gex/expirations/{symbol}")
  public Flux<LocalDate> getGEX(@PathVariable String symbol) {
    return gammaExposureService.fetchExpirationDates(symbol);
  }

  @GetMapping("/gex/history/datetimes/{symbol}")
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

  @GetMapping("/gex/history/{symbol}")
  public Mono<TotalGEX> getTotalGEXHistory(
      @PathVariable String symbol,
      @RequestParam(value = "dateTime", required = false) LocalDateTime dateTime) {
    return gammaExposureService.getTotalGEXCapture(symbol, dateTime == null ? LocalDateTime.now() : dateTime);
  }
}
