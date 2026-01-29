package com.kcjmowright.zerodte.service;

import com.pangility.schwab.api.client.accountsandtrading.SchwabAccountsAndTradingApiClient;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final SchwabAccountsAndTradingApiClient accountsAndTradingClient;
  private final SchwabApiClientTokenService tokenService;

  public Mono<Account> fetchAccount() {
    return accountsAndTradingClient
        .fetchAccountToMono(tokenService.getUserId(), tokenService.getEncryptedAccountHash(), "positions")
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }

}
