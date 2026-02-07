package com.kcjmowright.zerodte.model;

import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.kcjmowright.zerodte.config.MathConfig.MATH_CONTEXT;
import static com.kcjmowright.zerodte.math.BigDecimalAverage.average;
import static java.util.Objects.requireNonNull;

/**
 * An unbounded oscillator
 * <a href="https://www.investopedia.com/terms/c/commoditychannelindex.asp">Commodity Channel Index</a>
 */
@Getter
public class CommodityChannelIndex {

  private final int period;
  private final BigDecimal coefficient;
  private final List<QuoteEntity> quotes;
  private final List<IndicatorValue> values;

  private static final BigDecimal THREE = new BigDecimal("3.0", MATH_CONTEXT);
  public static final BigDecimal DEFAULT_COEFFICIENT = new BigDecimal("0.15", MATH_CONTEXT);
  public static final int DEFAULT_PERIOD = 20;

  public CommodityChannelIndex(List<QuoteEntity> quotes, int period, BigDecimal coefficient) {
    this.quotes = requireNonNull(quotes, "Expected quotes");
    this.period = period;
    this.coefficient = coefficient;
    this.values = new ArrayList<>();
    calculate();
  }

  public CommodityChannelIndex(List<QuoteEntity> quotes) {
    this(quotes, DEFAULT_PERIOD, DEFAULT_COEFFICIENT);
  }

  public void addQuote(QuoteEntity quote) {
    this.quotes.add(quote);
    if (this.period <= this.quotes.size()) {
      int idx = this.quotes.size();
      calculateSlice(this.quotes.subList(idx - this.period, idx));
    }
  }

  void calculate() {
    if (this.period <= this.quotes.size()) {
      for (int idx = this.period; idx <= this.quotes.size(); idx++) {
        calculateSlice(this.quotes.subList(idx - this.period, idx));
      }
    }
  }

  void calculateSlice(List<QuoteEntity> slice) {
    List<BigDecimal> typicalPrices = slice.stream().map(this::calculateTypicalPrice).toList();
    BigDecimal movingAverage = average(typicalPrices);
    List<BigDecimal> absDeviations = typicalPrices.stream().map(tp -> tp.subtract(movingAverage, MATH_CONTEXT).abs()).toList();
    BigDecimal meanDeviation = average(absDeviations);
    BigDecimal divisor = coefficient.multiply(meanDeviation, MATH_CONTEXT);
    BigDecimal cci = divisor.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : typicalPrices.getLast()
        .subtract(movingAverage, MATH_CONTEXT)
        .divide(divisor, MATH_CONTEXT);
    IndicatorValue value = new IndicatorValue(slice.getLast().getCreated(), cci, null);
    values.add(value);
  }

  BigDecimal calculateTypicalPrice(QuoteEntity quote) {
    return quote.getHigh().add(quote.getLow()).add(quote.getClose()).divide(THREE, MATH_CONTEXT);
  }
}
