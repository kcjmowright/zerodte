package com.kcjmowright.zerodte.algo;

import com.kcjmowright.zerodte.config.MathConfig;
import lombok.NonNull;

import java.math.BigDecimal;

public class GammaExposure {

  private static final BigDecimal HUNDRED = new BigDecimal("100.0000");
  private static final BigDecimal ONE_PERCENT = new BigDecimal("0.0100");

  public static BigDecimal callGEX(
      @NonNull BigDecimal gamma,
      @NonNull BigDecimal openInterest,
      @NonNull BigDecimal spotPrice) {

    return gamma
        .multiply(openInterest)
        .multiply(spotPrice)
        .multiply(HUNDRED)
        .multiply(ONE_PERCENT, MathConfig.MATH_CONTEXT);
  }

  public static BigDecimal putGEX(
      @NonNull BigDecimal gamma,
      @NonNull BigDecimal openInterest,
      @NonNull BigDecimal spotPrice) {

    return gamma
        .multiply(openInterest)
        .multiply(spotPrice)
        .multiply(HUNDRED)
        .multiply(ONE_PERCENT).negate(MathConfig.MATH_CONTEXT);
  }

}
