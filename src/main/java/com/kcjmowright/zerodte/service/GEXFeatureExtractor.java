package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.OptionContractGEX;
import com.kcjmowright.zerodte.model.TotalGEX;
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

  public GEXFeatures extractFeatures(TotalGEX snapshot, List<TotalGEX> historicalSnapshots) {

    BigDecimal currentPrice = snapshot.getSpotPrice();

    return GEXFeatures.builder()
        .distanceToCallWall(calculateDistance(currentPrice, snapshot.getCallWall()))
        .distanceToPutWall(calculateDistance(currentPrice, snapshot.getPutWall()))
        .distanceToFlipPoint(calculateDistance(currentPrice, snapshot.getFlipPoint()))
        .callPutGEXRatio(calculateGEXRatio(snapshot))
        .netGEX(snapshot.getTotalGEX())
        .gexSkew(calculateGEXSkew(snapshot))
        .concentrationIndex(calculateConcentration(snapshot))
        .relativePosition(calculateRelativePosition(snapshot))
        .minutesToExpiry(calculateMinutesToExpiry(snapshot.getTimestamp()))
        .priceVelocity(calculatePriceVelocity(historicalSnapshots))
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
    LocalDateTime expiry = timestamp.toLocalDate()
        .atTime(15, 0);
    return (int) Duration.between(timestamp, expiry).toMinutes();
  }

  private BigDecimal calculatePriceVelocity(List<TotalGEX> historical) {
    if (historical.size() < 2) {
      return BigDecimal.ZERO;
    }

    // Calculate average price change over last 5 minutes
    int lookback = Math.min(5, historical.size());
    List<TotalGEX> recent = historical.subList(
        historical.size() - lookback,
        historical.size()
    );

    BigDecimal totalChange = BigDecimal.ZERO;
    for (int i = 1; i < recent.size(); i++) {
      BigDecimal change = recent.get(i).getSpotPrice()
          .subtract(recent.get(i - 1).getSpotPrice());
      totalChange = totalChange.add(change);
    }

    return totalChange.divide(
        BigDecimal.valueOf(recent.size() - 1),
        4,
        RoundingMode.HALF_UP
    );
  }
}