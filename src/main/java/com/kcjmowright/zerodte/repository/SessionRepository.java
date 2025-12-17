package com.kcjmowright.zerodte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kcjmowright.zerodte.model.entity.SessionEntity;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

  SessionEntity findByUsername(String username);
  void deleteByUsername(String username);
}
