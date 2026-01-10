package com.kcjmowright.zerodte.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WalkForwardRequest {
  private String symbol;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int trainingWindow;
  private int testingWindow;
}
