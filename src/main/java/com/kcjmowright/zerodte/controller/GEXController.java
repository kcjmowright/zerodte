package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.service.GammaExposureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class GEXController {

  private final GammaExposureService gammaExposureService;

  @GetMapping("/gex/{symbol}")
  public Mono<TotalGEX> getGEX(
      @PathVariable String symbol,
      @RequestParam(value = "fromDate") LocalDate fromDate,
      @RequestParam(value = "toDate") LocalDate toDate) {
    return gammaExposureService.computeGammaExposure(symbol, fromDate, toDate);
  }
}
