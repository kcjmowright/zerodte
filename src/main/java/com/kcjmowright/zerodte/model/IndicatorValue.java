package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicatorValue {
  LocalDateTime timestamp;
  BigDecimal value;
  BigDecimal stdDev;
}