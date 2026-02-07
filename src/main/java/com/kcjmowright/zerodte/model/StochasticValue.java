package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StochasticValue {
  LocalDateTime date;

  /**
   * Current value.
   */
  BigDecimal k;

  /**
   * The 3 period moving average of k.
   */
  BigDecimal d;
}