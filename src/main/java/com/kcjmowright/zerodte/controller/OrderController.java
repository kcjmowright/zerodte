package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.service.OrderService;
import com.kcjmowright.zerodte.service.PriceService;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class OrderController {

  private final OrderService orderService;
  private final PriceService priceService;

  @GetMapping("/orders")
  public Flux<Order> getOrders(
      @RequestParam(value = "from", required = false) ZonedDateTime from,
      @RequestParam(value = "to", required = false) ZonedDateTime to) {
    return orderService.fetchOrders(
        from == null ? ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS) : from,
        to == null ? ZonedDateTime.now() : to);
  }

  @GetMapping("/option/zero-dte-iron-condor/order")
  public Mono<Order> getZeroDTEIronCondorOrder(
      @RequestParam(value = "symbol") String symbol,
      @RequestParam(value = "quantity", required = false, defaultValue = "1.0") BigDecimal quantity,
      @RequestParam(value = "putLongDelta", required = false, defaultValue = "-10.0") BigDecimal putLongDelta,
      @RequestParam(value = "putShortDelta", required = false, defaultValue = "-40.0") BigDecimal putShortDelta,
      @RequestParam(value = "callShortDelta", required = false, defaultValue = "40.0") BigDecimal callShortDelta,
      @RequestParam(value = "callLongDelta", required = false, defaultValue = "10.0") BigDecimal callLongDelta) {
    final LocalDate today = LocalDate.now();
    return orderService.buildIronCondorOrder(
        priceService.findIronCondor(
            symbol,
            today,
            today,
            putLongDelta,
            putShortDelta,
            callShortDelta,
            callLongDelta),
        quantity
    );
  }
}
