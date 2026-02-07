package com.kcjmowright.zerodte.math;

import java.math.BigDecimal;
import java.util.List;

import static com.kcjmowright.zerodte.config.MathConfig.MATH_CONTEXT;
import static com.kcjmowright.zerodte.math.BigDecimalSum.sum;

public class BigDecimalAverage {
  public static BigDecimal average(List<BigDecimal> values) {
    return sum(values).divide(new BigDecimal(values.size()), MATH_CONTEXT);
  }

  public static BigDecimal average(BigDecimal... values) {
    return sum(values).divide(new BigDecimal(values.length), MATH_CONTEXT);
  }
}
