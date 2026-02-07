package com.kcjmowright.zerodte.math;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.kcjmowright.zerodte.config.MathConfig.MATH_CONTEXT;

public class BigDecimalSum {
  public static BigDecimal sum(List<BigDecimal> values) {
    return values.stream().map(Objects::requireNonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public static BigDecimal sumOfSquares(List<BigDecimal> values) {
    return values.stream().map(Objects::requireNonNull).map(v -> v.pow(2, MATH_CONTEXT)).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public static BigDecimal sum(BigDecimal ...values) {
    return Arrays.stream(values).map(Objects::requireNonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
