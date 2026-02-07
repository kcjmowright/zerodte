package com.kcjmowright.zerodte.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Define an interface whose getter method names match the column aliases in a native query
 */
public interface GEXDataProjection {
  LocalDateTime getCreated();
  String getSymbol();
  BigDecimal getOpen();
  BigDecimal getClose();
  BigDecimal getHigh();
  BigDecimal getLow();
  String getTotalGEX();
  BigDecimal getVix();
}
