package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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
