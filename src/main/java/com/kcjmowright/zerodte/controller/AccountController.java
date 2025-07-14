package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.service.ZeroDTEAgentService;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Account;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController()
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class AccountController {
  private final ZeroDTEAgentService agentService;

  @GetMapping("/account")
  public Mono<Account> getAccount() {
    return agentService.fetchAccount();
  }

  @GetMapping("/orders")
  public Flux<Order> getOrders() {
    return agentService.fetchOrders();
  }
}
