package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackTestTrade {
  private LocalDateTime entryTime;
  private BigDecimal entryPrice;
  private LocalDateTime exitTime;
  private BigDecimal actualPrice;
  private BigDecimal predictedPrice;
  private String direction;
  private String regime;
  private BigDecimal confidence;
}
