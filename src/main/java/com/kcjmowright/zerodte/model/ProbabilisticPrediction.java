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
public class ProbabilisticPrediction {
  private LocalDateTime predictionTime;
  private LocalDateTime targetTime;
  private BigDecimal meanPrediction;
  private BigDecimal stdDeviation;
  private BigDecimal confidence95Lower;
  private BigDecimal confidence95Upper;
}
