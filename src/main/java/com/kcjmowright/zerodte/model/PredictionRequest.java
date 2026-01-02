package com.kcjmowright.zerodte.model;

import lombok.Data;

@Data
public class PredictionRequest {
  private int minutesAhead;
  private String symbol;
}
