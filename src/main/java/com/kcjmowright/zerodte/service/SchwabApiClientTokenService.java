package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.SessionEntity;
import com.kcjmowright.zerodte.repository.SessionRepository;
import com.pangility.schwab.api.client.oauth2.SchwabAccount;
import com.pangility.schwab.api.client.oauth2.SchwabTokenHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchwabApiClientTokenService implements SchwabTokenHandler {
  private final SessionRepository sessionRepository;

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
}
