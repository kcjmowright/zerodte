package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.service.AccountService;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Account;
import com.pangility.schwab.api.client.accountsandtrading.model.account.MarginAccount;
import com.pangility.schwab.api.client.accountsandtrading.model.account.SecuritiesAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AccountController.class)
public class AccountControllerTest {

  @MockitoBean
  private AccountService accountService;

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testGetAccount() {
    final Account account = new Account();
    MarginAccount marginAccount = new MarginAccount();
    marginAccount.setAccountNumber("foo");
    marginAccount.setType(SecuritiesAccount.Type.MARGIN);
    marginAccount.setIsDayTrader(true);
    account.setSecuritiesAccount(marginAccount);

    when(accountService.fetchAccount())
        .thenReturn(Mono.just(account));

    webTestClient.get()
        .uri("/api/v1/account")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.securitiesAccount.accountNumber").isEqualTo("foo")
        .jsonPath("$.securitiesAccount.type").isEqualTo("MARGIN")
        .jsonPath("$.securitiesAccount.isDayTrader").isEqualTo(true);

    verify(accountService, times(1)).fetchAccount();
  }
}
