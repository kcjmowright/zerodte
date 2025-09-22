package com.kcjmowright.zerodte.model;

import ch.obermuhlner.math.big.BigDecimalMath;
import com.kcjmowright.zerodte.config.MathConfig;

import java.math.BigDecimal;
import java.util.Scanner;

public class BlackScholes {

  private static final BigDecimal a1 = new BigDecimal("0.254829592");
  private static final BigDecimal a2 = new BigDecimal("-0.284496736");
  private static final BigDecimal a3 = new BigDecimal("1.421413741");
  private static final BigDecimal a4 = new BigDecimal("-1.453152027");
  private static final BigDecimal a5 = new BigDecimal("1.061405429");
  private static final BigDecimal p = new BigDecimal("0.3275911");
  private static final BigDecimal HALF = new BigDecimal("0.5");

  /**
   * Calculates the Black-Scholes option price.
   *
   * @param callPut CALL or PUT
   * @param stockPrice Current stock price
   * @param strikePrice Strike price
   * @param time Time to expiration in years
   * @param riskFree Risk-free interest rate
   * @param volatility Volatility
   * @return The calculated option price to 4 decimals.
   */
  public static BigDecimal calculate(
      CallPut callPut,
      BigDecimal stockPrice,
      BigDecimal strikePrice,
      BigDecimal time,
      BigDecimal riskFree,
      BigDecimal volatility) {

    // d1 = (Math.log(S / K) + (r + 0.5 * v * v) * T) / (v * Math.sqrt(T));
    BigDecimal d1 = BigDecimalMath.log(stockPrice.divide(strikePrice, MathConfig.MATH_CONTEXT), MathConfig.MATH_CONTEXT)
        .add(riskFree.add(HALF.multiply(volatility).multiply(volatility)).multiply(time))
        .divide(volatility.multiply(time.sqrt(MathConfig.MATH_CONTEXT)), MathConfig.MATH_CONTEXT);
    // d2 = d1 - v * Math.sqrt(T)
    BigDecimal d2 = d1.subtract(
        volatility.multiply(time.sqrt(MathConfig.MATH_CONTEXT))
    );

    return switch(callPut) {
      // S * cumulativeNormalDistribution(d1) - K * Math.exp(-r * T) * cumulativeNormalDistribution(d2)
      case CALL -> stockPrice.multiply(cumulativeNormalDistribution(d1))
          .subtract(strikePrice
              .multiply(BigDecimalMath.exp(riskFree.negate().multiply(time), MathConfig.MATH_CONTEXT))
              .multiply(cumulativeNormalDistribution(d2))
          ).setScale(4, MathConfig.MATH_CONTEXT.getRoundingMode());
      // K * Math.exp(-r * T) * cumulativeNormalDistribution(-d2) - S * cumulativeNormalDistribution(-d1)
      case PUT -> strikePrice
          .multiply(BigDecimalMath.exp(riskFree.negate().multiply(time), MathConfig.MATH_CONTEXT))
          .multiply(cumulativeNormalDistribution(d2.negate()))
          .subtract(stockPrice.multiply(cumulativeNormalDistribution(d1.negate())))
          .setScale(4, MathConfig.MATH_CONTEXT.getRoundingMode());
    };
  }

  /**
   * Calculates an approximate cumulative normal distribution.
   *
   * @param x calculate cumulative normal distribution for x.
   * @return The approximated cumulative normal distribution
   */
  static BigDecimal cumulativeNormalDistribution(BigDecimal x) {
    BigDecimal x1 = x.abs().divide(BigDecimal.TWO.sqrt(MathConfig.MATH_CONTEXT), MathConfig.MATH_CONTEXT);
    // t = 1.0 / (1.0 + p * x);
    BigDecimal t = BigDecimal.ONE.divide(BigDecimal.ONE.add(p.multiply(x1)), MathConfig.MATH_CONTEXT);
    // 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
    BigDecimal y =
        BigDecimal.ONE.subtract(
            a5.multiply(t).add(a4)
                .multiply(t).add(a3)
                .multiply(t).add(a2)
                .multiply(t).add(a1)
                .multiply(t)
                .multiply(BigDecimalMath.exp(x1.negate().multiply(x1), MathConfig.MATH_CONTEXT))
        );

    // 0.5 * (1.0 + sign * y);
    return x.compareTo(BigDecimal.ZERO) < 0 ?
        HALF.multiply(BigDecimal.ONE.add(y.negate())) : HALF.multiply(BigDecimal.ONE.add(y));
  }

  public static void main(String[] args) {
    try (Scanner scanner = new Scanner(System.in)) {
      System.out.println(BlackScholes.calculate(CallPut.valueOf(scanner.next()), new BigDecimal(scanner.next()),
          new BigDecimal(scanner.next()),
          new BigDecimal(scanner.next()),
          new BigDecimal(scanner.next()),
          new BigDecimal(scanner.next())));
    } catch (Exception e) {
      System.err.printf("""
            Expected:
            (CALL|PUT) <stock price> <strike price> <time > <risk free> <volatility>
            but errored with:
            %s
          %n""", e.getMessage());
    }
  }
}