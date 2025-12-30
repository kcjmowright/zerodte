package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.entity.SessionEntity;
import com.pangility.schwab.api.client.accountsandtrading.SchwabAccountsAndTradingApiClient;
import com.pangility.schwab.api.client.common.EnableSchwabApi;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.oauth2.SchwabAccount;
import com.pangility.schwab.api.client.oauth2.SchwabTokenHandler;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@EnableSchwabApi
@Slf4j
public class SchwabApiClientTokenService implements SchwabTokenHandler {
  private final SessionService sessionService;
  private final SchwabAccountsAndTradingApiClient accountsAndTradingClient;
  private final SchwabMarketDataApiClient marketDataClient;

  @Getter
  @Value("${zerodte.agent.userId}")
  private String userId;

  @Value("${zerodte.agent.accountNumber}")
  private String accountNumber;

  private String encryptedAccountHash;

  @Override
  public void onAccessTokenChange(SchwabAccount schwabAccount) {
    SessionEntity session = sessionService.findByUsername(userId);
    if (session == null) {
      session = new SessionEntity();
    }
    LocalDateTime now = LocalDateTime.now();
    session.setLastUpdated(now);
    session.setCreated(now);
    session.setUsername(userId);
    session.setToken(schwabAccount.getAccessToken());
    session.setAccessExpiration(schwabAccount.getAccessExpiration());
    session.setRefreshToken(schwabAccount.getRefreshToken());
    session.setRefreshExpiration(schwabAccount.getRefreshExpiration());
    sessionService.save(session);
  }

  @Override
  public void onRefreshTokenChange(SchwabAccount schwabAccount) {
    SessionEntity session = sessionService.findByUsername(schwabAccount.getUserId());
    LocalDateTime now = LocalDateTime.now();
    if (session == null) {
      session = new SessionEntity();
      session.setCreated(now);
    }
    session.setUsername(userId);
    session.setToken(schwabAccount.getAccessToken());
    session.setAccessExpiration(schwabAccount.getAccessExpiration());
    session.setRefreshToken(schwabAccount.getRefreshToken());
    session.setRefreshExpiration(schwabAccount.getRefreshExpiration());
    session.setLastUpdated(now);
    sessionService.save(session);
  }

  public String getEncryptedAccountHash() {
    if (encryptedAccountHash == null) {
      encryptedAccountHash = accountsAndTradingClient.fetchEncryptedAccountsToFlux(userId)
          .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)))
          .toStream()
          .filter(account -> Objects.equals(account.getAccountNumber(), accountNumber))
          .findFirst()
          .orElseThrow()
          .getHashValue();
    }
    return encryptedAccountHash;
  }

  @PostConstruct
  public void init() {
    if (!(accountsAndTradingClient.isInitialized() && marketDataClient.isInitialized())) {
      SessionEntity session = sessionService.findByUsername(userId);
      SchwabAccount schwabAccount = new SchwabAccount();
      schwabAccount.setUserId(userId);
      if (session != null) {
        log.warn("Using existing session");
        schwabAccount.setAccessToken(session.getToken());
        schwabAccount.setAccessExpiration(session.getAccessExpiration());
        schwabAccount.setRefreshToken(session.getRefreshToken());
        schwabAccount.setRefreshExpiration(session.getRefreshExpiration());
      } else {
        log.warn("No existing session");
      }
      if (!accountsAndTradingClient.isInitialized()) {
        accountsAndTradingClient.init(schwabAccount, this);
      }
      if (!marketDataClient.isInitialized()) {
        marketDataClient.init(schwabAccount, this);
      }
    }
  }
}
