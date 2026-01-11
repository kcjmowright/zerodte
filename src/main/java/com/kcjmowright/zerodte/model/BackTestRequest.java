package com.kcjmowright.zerodte.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BackTestRequest {
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int predictionHorizon;
  private int minHistorySize;
  private String symbol;
}
