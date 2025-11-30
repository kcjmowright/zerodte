package com.kcjmowright.zerodte.model;

import com.kcjmowright.zerodte.algo.GammaExposure;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
public class TotalGEX {
  private Map<BigDecimal, OptionContractGEX> gexPerStrike = new TreeMap<>(Comparator.reverseOrder());
  private BigDecimal totalCallGEX = BigDecimal.ZERO;
  private BigDecimal totalPutGEX = BigDecimal.ZERO;
  private BigDecimal totalGEX = BigDecimal.ZERO;
  private BigDecimal callWall = BigDecimal.ZERO;
  private BigDecimal putWall = BigDecimal.ZERO;
  private BigDecimal flipPoint = BigDecimal.ZERO;

  /**
   * Calculate GEX values from options contracts.
   * @param optionContracts a {@link Stream} of {@link OptionContract} elements.
   * @param spotPrice current underlying price
   * @param suppressDetails do not include contract details
   * @return total GEX computations
   */
  public static TotalGEX fromOptionContracts(
      @NonNull Stream<OptionContract> optionContracts,
      @NonNull BigDecimal spotPrice,
      boolean suppressDetails) {

    final TotalGEX totalGEX = new TotalGEX();
    final Map<BigDecimal, BigDecimal> gexAboveSpot = new TreeMap<>(); // Track Total GEX above spot price
    final Map<BigDecimal, BigDecimal> gexBelowSpot = new TreeMap<>(); // Track Total GEX below spot price

    optionContracts.forEach(contract -> {
      OptionContractGEX optionContractGEX = totalGEX.getGexPerStrike()
          .computeIfAbsent(contract.getStrikePrice(), $ -> new OptionContractGEX(contract.getStrikePrice()));

      if (contract.getPutCall() == OptionContract.PutCall.PUT) { // PUTs
        if (!suppressDetails) {
          optionContractGEX.getContracts().add(contract);
        }
        optionContractGEX.setPutGEX(optionContractGEX.getPutGEX()
            .add(GammaExposure.putGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice)));
        totalGEX.setTotalPutGEX(totalGEX.getTotalPutGEX().add(optionContractGEX.getPutGEX()));
      } else { // CALLs
        if (!suppressDetails) {
          optionContractGEX.getContracts().add(contract);
        }
        optionContractGEX.setCallGEX(optionContractGEX.getCallGEX()
            .add(GammaExposure.callGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice)));
        totalGEX.setTotalCallGEX(totalGEX.getTotalCallGEX().add(optionContractGEX.getCallGEX()));
      }
      optionContractGEX.setTotalGEX(optionContractGEX.getCallGEX().add(optionContractGEX.getPutGEX()));
      if (spotPrice.compareTo(contract.getStrikePrice()) > 0) { // If spot price is GT the strike price
        gexBelowSpot.put(contract.getStrikePrice(), optionContractGEX.getTotalGEX());
      } else if (spotPrice.compareTo(contract.getStrikePrice()) < 0) { // If spot price LT the strike price
        gexAboveSpot.put(contract.getStrikePrice(), optionContractGEX.getTotalGEX());
      }
    });
    // Calculate total GEX
    totalGEX.setTotalGEX(totalGEX.getTotalCallGEX().add(totalGEX.getTotalPutGEX()));
    // Find CALL wall
    gexAboveSpot.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
        .ifPresent(totalGEX::setCallWall);
    // Find PUT wall
    gexBelowSpot.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
        .ifPresent(totalGEX::setPutWall);
    // Find flip point
    calculateFlipPoint(List.copyOf(totalGEX.getGexPerStrike().entrySet())).ifPresent(totalGEX::setFlipPoint);
    return totalGEX;
  }

  private static Optional<BigDecimal> calculateFlipPoint(List<Map.Entry<BigDecimal, OptionContractGEX>> numbers) {
    if (numbers == null || numbers.size() < 2) {
      return Optional.empty();
    }
    for (int i = numbers.size(); --i > 0;) {
      Map.Entry<BigDecimal, OptionContractGEX> previous = numbers.get(i - 1);
      Map.Entry<BigDecimal, OptionContractGEX> current = numbers.get(i);
      if ((previous.getValue().getTotalGEX().signum() > 0 && current.getValue().getTotalGEX().signum() < 0)
          || (previous.getValue().getTotalGEX().signum() < 0 && current.getValue().getTotalGEX().signum() > 0)) {
        return Optional.of(previous.getKey());
      }
    }
    return Optional.empty();
  }

//  public static Optional<Integer> findFirstSignChangeStream(List<Integer> numbers) {
//    return numbers.stream()
//        // Creates a stream of List<Integer> where each list contains [previous, current]
//        .gather(Gatherers.windowSliding(2))
//        // Filter for the pair where a sign change occurred
//        .filter(pair -> {
//          int previous = pair.get(0);
//          int current = pair.get(1);
//          // Check for opposite signs (excluding zero as a sign changer)
//          return (previous > 0 && current < 0) || (previous < 0 && current > 0);
//        })
//        // Map to get the current value (the second element of the pair)
//        .map(pair -> pair.get(1))
//        // Find the first occurrence
//        .findFirst();
//  }

}
