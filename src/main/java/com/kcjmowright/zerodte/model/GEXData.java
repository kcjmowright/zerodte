package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GEXData {
  private LocalDateTime created;
  private String symbol;
  private BigDecimal low;
  private BigDecimal high;
  private BigDecimal open;
  private BigDecimal close;
  private TotalGEX totalGEX;
  private BigDecimal vix;
}
