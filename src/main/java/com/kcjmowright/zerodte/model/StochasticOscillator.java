package com.kcjmowright.zerodte.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.kcjmowright.zerodte.config.MathConfig.MATH_CONTEXT;
import static com.kcjmowright.zerodte.math.BigDecimalAverage.average;

/**
 * <code>k = (Most Recent Price - Period Low) / (Period High - Period Low) * 100.0</code>
 * <p>
 * Where:<ul>
 * <li>k = the current calculated value.
 * <li>d = 3 - period simple moving average of k.
 */
@Getter
@Slf4j
public class StochasticOscillator {

  /**
   * The calculated values.
   */
  private final List<StochasticValue> stochasticValues = new ArrayList<>();
  private final List<QuoteEntity> quotes;
  private final int period;

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0", MATH_CONTEXT);
  public static final int DEFAULT_PERIOD = 14;

  public StochasticOscillator(List<QuoteEntity> quotes, int period) {
    this.quotes = Objects.requireNonNull(quotes);
    this.period = period;
    calculate();
  }

  public StochasticOscillator(List<QuoteEntity> quotes) {
    this(quotes, DEFAULT_PERIOD);
  }

  public StochasticOscillator() {
    this(new ArrayList<>());
  }

  static BigDecimal k(BigDecimal close, BigDecimal min, BigDecimal max) {
    BigDecimal divisor = max.subtract(min);
    return divisor.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
        close.subtract(min).divide(divisor, MATH_CONTEXT).multiply(ONE_HUNDRED, MATH_CONTEXT);
  }

  static BigDecimal d(List<StochasticValue> stochasticValues) {
    return average(stochasticValues.subList(stochasticValues.size() - 3, stochasticValues.size()).stream().map(StochasticValue::getK).toList());
  }

  /**
   * Calculates k and d values for `this.quotes`.
   */
  void calculate() {
    if (this.period <= this.quotes.size()) {
      for (int idx = this.period; idx <= this.quotes.size(); idx++) {
        List<QuoteEntity> slice = this.quotes.subList(idx - this.period, idx);
        BigDecimal[] minMax = this.calculateMinMax(slice);
        QuoteEntity quote = slice.getLast();
        StochasticValue stochasticValue = new StochasticValue(quote.getCreated(), k(quote.getClose(), minMax[0], minMax[1]), null);
        if (this.stochasticValues.size() >= 3) {
          this.stochasticValues.add(stochasticValue);
          stochasticValue.d = d(stochasticValues);
        }
      }
    }
  }

  /**
   * Modifies `this` object by calculating a new value for k and d and pushing the result onto `this.values`.
   *
   * @param quote a Quote object.
   */
  public void addQuote(QuoteEntity quote) {
    this.quotes.add(quote);
    if (this.period <= this.quotes.size()) {
      List<QuoteEntity> slice = this.quotes.subList(this.quotes.size() - this.period, this.quotes.size());
      BigDecimal[] minMax = calculateMinMax(slice);
      StochasticValue stochasticValue = new StochasticValue(quote.getCreated(), k(quote.getClose(), minMax[0], minMax[1]), null);

      this.stochasticValues.add(stochasticValue);
      if (this.stochasticValues.size() >= 3) {
        stochasticValue.d = d(this.stochasticValues);
      }
    }
  }

  /**
   * @param quotes a range of quotes.
   * @return BigDecimal[] the min and max of the given quotes.
   */
  private BigDecimal[] calculateMinMax(List<QuoteEntity> quotes) {
    BigDecimal max = null;
    BigDecimal min = null;
    for (QuoteEntity quote : quotes) {
      if (max == null || max.compareTo(quote.getHigh()) < 0) {
        max = quote.getHigh();
      }
      if (min == null || min.compareTo(quote.getLow()) > 0) {
        min = quote.getLow();
      }
    }
    return new BigDecimal[] {min, max};
  }
}
