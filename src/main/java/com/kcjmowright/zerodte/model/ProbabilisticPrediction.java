package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProbabilisticPrediction {
  private LocalDateTime predictionTime;
  private LocalDateTime targetTime;
  private BigDecimal meanPrediction;
  private BigDecimal stdDeviation;
  private BigDecimal confidence95Lower;
  private BigDecimal confidence95Upper;
}
