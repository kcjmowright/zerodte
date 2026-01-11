package com.kcjmowright.zerodte.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WalkForwardRequest {
  private String symbol;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int trainingWindow;
  private int testingWindow;
}
