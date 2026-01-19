package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockPriceUpdate {
  private String symbol;
  private BigDecimal currentPrice;
  private LocalDateTime timestamp;
  private Map<Integer, PricePrediction> predictions;
}
