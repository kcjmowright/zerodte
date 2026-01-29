package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.service.AccountService;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController()
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AccountController {

  private final AccountService accountService;

  @GetMapping("/account")
  public Mono<Account> getAccount() {
    return accountService.fetchAccount();
  }

}
