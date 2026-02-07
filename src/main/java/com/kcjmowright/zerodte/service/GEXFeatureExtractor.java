package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.CommodityChannelIndex;
import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.OptionContractGEX;
import com.kcjmowright.zerodte.model.StochasticOscillator;
import com.kcjmowright.zerodte.model.StochasticValue;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class GEXFeatureExtractor {

  public GEXFeatures extractFeatures(GEXData snapshot, List<GEXData> historicalSnapshots) {

    TotalGEX totalGEX = snapshot.getTotalGEX();
    BigDecimal currentPrice = totalGEX.getSpotPrice();
    BigDecimal velocityEnd = calculatePriceVelocity(historicalSnapshots, 5, 0);
    BigDecimal velocityPrev = calculatePriceVelocity(historicalSnapshots, 6, 1);

    return GEXFeatures.builder()
        .distanceToCallWall(calculateDistance(currentPrice, totalGEX.getCallWall()))
        .distanceToPutWall(calculateDistance(currentPrice, totalGEX.getPutWall()))
        .distanceToFlipPoint(calculateDistance(currentPrice, totalGEX.getFlipPoint()))
        .callPutGEXRatio(calculateGEXRatio(totalGEX))
        .netGEX(totalGEX.getTotalGEX())
        .gexSkew(calculateGEXSkew(totalGEX))
        .concentrationIndex(calculateConcentration(totalGEX))
        .relativePosition(calculateRelativePosition(totalGEX))
        .minutesToExpiry(calculateMinutesToExpiry(totalGEX.getTimestamp()))
        .priceVelocity(velocityEnd)
        .priceAcceleration(velocityEnd.subtract(velocityPrev))
        .cci(calculateCci(historicalSnapshots))
        .stochastic(calculateStochasticDivergence(historicalSnapshots))
        .build();
  }

  private BigDecimal calculateDistance(BigDecimal current, BigDecimal target) {
    return target == null ?
        BigDecimal.ZERO:
        target.subtract(current).divide(current, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100")); // percentage
  }

  private BigDecimal calculateGEXRatio(TotalGEX snapshot) {
    BigDecimal putGEX = snapshot.getTotalPutGEX().abs();
    return putGEX.compareTo(BigDecimal.ZERO) == 0 ?
        BigDecimal.valueOf(999) :
        snapshot.getTotalCallGEX().divide(putGEX, 4, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateGEXSkew(TotalGEX snapshot) {
    // Measures asymmetry in GEX distribution
    BigDecimal currentPrice = snapshot.getSpotPrice();
    BigDecimal atmGEX = snapshot.getGexPerStrike()
        .entrySet()
        .stream()
        .min(Comparator.comparing(e ->
            e.getKey().subtract(currentPrice).abs()))
        .map(Map.Entry::getValue)
        .map(OptionContractGEX::getTotalGEX)
        .orElse(BigDecimal.ZERO);

    BigDecimal totalAbsGEX = snapshot.getGexPerStrike()
        .values()
        .stream()
        .map(OptionContractGEX::getTotalGEX)
        .map(BigDecimal::abs)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return totalAbsGEX.compareTo(BigDecimal.ZERO) == 0 ?
        BigDecimal.ZERO :
        atmGEX.divide(totalAbsGEX, 4, RoundingMode.HALF_UP);
  }

  /**
   * Herfindahl index (also called the Herfindahl-Hirschman Index or HHI) is a measure of market concentration.
   * Square each values share (expressed as a percentage or decimal) and add them all together.
   * @param snapshot {@link TotalGEX}
   * @return the concentration value.
   */
  private BigDecimal calculateConcentration(TotalGEX snapshot) {
    return snapshot.getTotalGEX().compareTo(BigDecimal.ZERO) == 0 ?
        BigDecimal.ZERO :
        snapshot.getGexPerStrike().values().stream()
          .map(gex -> gex.getTotalGEX().divide(snapshot.getTotalGEX(), 6, RoundingMode.HALF_UP).pow(2))
          .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateRelativePosition(TotalGEX snapshot) {
    // Where price sits between put wall (0) and call wall (1)
    BigDecimal putWall = snapshot.getPutWall();
    BigDecimal callWall = snapshot.getCallWall();
    BigDecimal current = snapshot.getSpotPrice();

    return callWall.compareTo(putWall) == 0 ?
      BigDecimal.valueOf(0.5) :
      current.subtract(putWall).divide(callWall.subtract(putWall), 4, RoundingMode.HALF_UP);
  }

  private Integer calculateMinutesToExpiry(LocalDateTime timestamp) {
    // 0DTE options expire at 3:00 PM CT (15:00)
    LocalDateTime expiry = timestamp.toLocalDate().atTime(15, 0);
    return (int) Duration.between(timestamp, expiry).toMinutes();
  }

  private BigDecimal calculatePriceVelocity(List<GEXData> historical, int startLookback, int endLookback) {
    if (historical.size() < 2) {
      return BigDecimal.ZERO;
    }

    // Calculate average price change over last 5 minutes
    int start = historical.size() - Math.min(startLookback, historical.size());
    int end = (historical.size() < startLookback) ? historical.size() : historical.size() - endLookback;
    List<GEXData> recent = historical.subList(start, end);

    BigDecimal totalChange = BigDecimal.ZERO;
    for (int i = 1; i < recent.size(); i++) {
      BigDecimal change = recent.get(i).getTotalGEX().getSpotPrice()
          .subtract(recent.get(i - 1).getTotalGEX().getSpotPrice());
      totalChange = totalChange.add(change);
    }

    return totalChange.divide(
        BigDecimal.valueOf(recent.size() - 1),
        4,
        RoundingMode.HALF_UP
    );
  }

  private BigDecimal calculateCci(List<GEXData> historicalSnapshots) {
    if (historicalSnapshots == null || historicalSnapshots.isEmpty()) {
      return BigDecimal.ZERO;
    }
    int first = Math.max(historicalSnapshots.size() - CommodityChannelIndex.DEFAULT_PERIOD * 2, 0);
    CommodityChannelIndex cci = new CommodityChannelIndex(historicalSnapshots
        .subList(first, historicalSnapshots.size())
        .stream()
        .map(data -> QuoteEntity.builder()
            .low(data.getLow())
            .high(data.getHigh())
            .open(data.getOpen())
            .close(data.getClose())
            .build()).toList());
    return cci.getValues().isEmpty() ? BigDecimal.ZERO : cci.getValues().getLast().getValue();
  }

  private BigDecimal calculateStochasticDivergence(List<GEXData> historicalSnapshots) {
    if (historicalSnapshots == null || historicalSnapshots.isEmpty()) {
      return BigDecimal.ZERO;
    }
    int first = Math.max(historicalSnapshots.size() - StochasticOscillator.DEFAULT_PERIOD * 2, 0);
    StochasticOscillator oscillator = new StochasticOscillator(historicalSnapshots
        .subList(first, historicalSnapshots.size())
        .stream()
        .map(data -> QuoteEntity.builder()
            .low(data.getLow())
            .high(data.getHigh())
            .open(data.getOpen())
            .close(data.getClose())
            .build()).toList());
    if (oscillator.getStochasticValues().isEmpty()) {
      return BigDecimal.ZERO;
    }
    StochasticValue value = oscillator.getStochasticValues().getLast();
    return value.getK().subtract(value.getD());
  }
}