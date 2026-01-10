package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PricePrediction {
  private LocalDateTime predictionTime;
  private LocalDateTime targetTime;
  private BigDecimal predictedPrice;
  private BigDecimal confidence;
  private String direction; // UP, DOWN, NEUTRAL
  private BigDecimal expectedMove;
  private String regime; // POSITIVE_GEX, NEGATIVE_GEX
}
