package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.SessionEntity;
import com.kcjmowright.zerodte.repository.SessionRepository;
import com.pangility.schwab.api.client.accountsandtrading.SchwabAccountsAndTradingApiClient;
import com.pangility.schwab.api.client.common.EnableSchwabApi;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.oauth2.SchwabAccount;
import com.pangility.schwab.api.client.oauth2.SchwabTokenHandler;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@EnableSchwabApi
public class SchwabApiClientTokenService implements SchwabTokenHandler {
  private final SessionRepository sessionRepository;
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
    sessionRepository.deleteAll();
    LocalDateTime now = LocalDateTime.now();
    SessionEntity session = new SessionEntity();
    session.setToken(schwabAccount.getAccessToken());
    session.setAccessExpiration(schwabAccount.getAccessExpiration());
    session.setRefreshToken(schwabAccount.getRefreshToken());
    session.setRefreshExpiration(schwabAccount.getRefreshExpiration());
    session.setLastUpdated(now);
    session.setCreated(now);
    sessionRepository.save(session);
  }

  @Override
  public void onRefreshTokenChange(SchwabAccount schwabAccount) {
    List<SessionEntity> sessions = sessionRepository.findAll();
    LocalDateTime now = LocalDateTime.now();
    SessionEntity session;
    if (sessions.isEmpty()) {
      session = new SessionEntity();
      session.setCreated(now);
    } else {
      session = sessions.getFirst();
    }
    session.setToken(schwabAccount.getAccessToken());
    session.setAccessExpiration(schwabAccount.getAccessExpiration());
    session.setRefreshToken(schwabAccount.getRefreshToken());
    session.setRefreshExpiration(schwabAccount.getRefreshExpiration());
    session.setLastUpdated(now);
    sessionRepository.save(session);
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
      List<SessionEntity> sessions = sessionRepository.findAll();
      SchwabAccount schwabAccount = new SchwabAccount();
      schwabAccount.setUserId(userId);
      if (!sessions.isEmpty()) {
        SessionEntity session = sessions.getFirst();
        schwabAccount.setAccessToken(session.getToken());
        schwabAccount.setAccessExpiration(session.getAccessExpiration());
        schwabAccount.setRefreshToken(session.getRefreshToken());
        schwabAccount.setRefreshExpiration(session.getRefreshExpiration());
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
