package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class BackTestResult {
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer totalPredictions;
  private BigDecimal accuracy;
  private BigDecimal meanAbsoluteError;
  private BigDecimal rmse;
  private BigDecimal profitFactor;
  private Map<String, BigDecimal> regimePerformance;
}
