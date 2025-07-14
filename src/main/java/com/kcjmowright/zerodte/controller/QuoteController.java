package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.IronCondorContracts;
import com.kcjmowright.zerodte.service.ZeroDTEAgentService;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainResponse;
import com.pangility.schwab.api.client.marketdata.model.movers.MoversRequest;
import com.pangility.schwab.api.client.marketdata.model.movers.Screener;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class QuoteController {
  private final ZeroDTEAgentService agentService;

  @GetMapping("/quote")
  public Mono<QuoteResponse> getQuote(@RequestParam String symbol) {
    return agentService.getQuote(symbol);
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
    return agentService.callMovers(MoversRequest.IndexSymbol.valueOf(indexSymbol));
  }

  @GetMapping("/option/chain/{symbol}")
  public Mono<OptionChainResponse> getOptionChain(
      @PathVariable String symbol,
      @RequestParam(value = "fromDate") LocalDate fromDate,
      @RequestParam(value = "toDate") LocalDate toDate) {
    return agentService.getOptionChain(symbol, fromDate, toDate);
  }

  @GetMapping("/option/zero-dte-iron-condor")
  public Mono<IronCondorContracts> getZeroDTEIronCondor(@RequestParam String symbol) {
    final LocalDate today = LocalDate.now();
    return agentService.findIronCondor(symbol, today, today);
  }

  @GetMapping("/option/zero-dte-iron-condor/order")
  public Mono<Order> getZeroDTEIronCondorOrder(@RequestParam String symbol) {
    final LocalDate today = LocalDate.now();
    return agentService.buildIronCondorOrder(agentService.findIronCondor(symbol, today, today));
  }

  @GetMapping("/option/iron-condor")
  public Mono<IronCondorContracts> getIronCondor(@RequestParam("symbol") String symbol, @RequestParam("dte") LocalDate dte) {
    return agentService.findIronCondor(symbol, dte, dte);
  }
}
