package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.entity.SessionEntity;
import com.kcjmowright.zerodte.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService {
  private final SessionRepository sessionRepository;

  public SessionEntity findByUsername(String username) {
    return sessionRepository.findByUsername(username);
  }

  public void deleteByUsername(String username) {
    sessionRepository.deleteByUsername(username);
  }
}
