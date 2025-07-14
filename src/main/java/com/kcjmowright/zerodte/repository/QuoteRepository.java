package com.kcjmowright.zerodte.repository;

import com.kcjmowright.zerodte.model.QuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {
}
