package com.kcjmowright.zerodte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kcjmowright.zerodte.model.SessionEntity;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
}
