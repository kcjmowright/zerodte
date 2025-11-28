package com.kcjmowright.zerodte.model;

import com.kcjmowright.zerodte.algo.GammaExposure;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
public class TotalGEX {
  private Map<BigDecimal, OptionContractGEX> gexPerStrike = new TreeMap<>(Comparator.naturalOrder());
  private BigDecimal totalCallGEX = BigDecimal.ZERO;
  private BigDecimal totalPutGEX = BigDecimal.ZERO;
  private BigDecimal totalGEX = BigDecimal.ZERO;
  private BigDecimal callWall = BigDecimal.ZERO;
  private BigDecimal putWall = BigDecimal.ZERO;

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
    final Map<BigDecimal, BigDecimal> callGexPerStrike = new TreeMap<>(); // Track CALL GEX above spot price
    final Map<BigDecimal, BigDecimal> putGexPerStrike = new TreeMap<>(); // Track PUT GEX below spot price

    optionContracts.forEach(contract -> {
      OptionContractGEX optionContractGEX = totalGEX.getGexPerStrike()
          .computeIfAbsent(contract.getStrikePrice(), $ -> new OptionContractGEX(contract.getStrikePrice()));

      if (contract.getPutCall() == OptionContract.PutCall.PUT) { // PUTs
        if (!suppressDetails) {
          optionContractGEX.getContracts().add(contract);
        }
        optionContractGEX.setPutGEX(optionContractGEX.getPutGEX()
            .add(GammaExposure.putGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice)));
        if (spotPrice.compareTo(contract.getStrikePrice()) >= 0) { // If spot price is GTE the strike price
          putGexPerStrike.merge(contract.getStrikePrice(), optionContractGEX.getPutGEX(), BigDecimal::add);
        }
        totalGEX.setTotalPutGEX(totalGEX.getTotalPutGEX().add(optionContractGEX.getPutGEX()));
      } else { // CALLs
        if (!suppressDetails) {
          optionContractGEX.getContracts().add(contract);
        }
        optionContractGEX.setCallGEX(optionContractGEX.getCallGEX()
            .add(GammaExposure.callGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice)));
        if (spotPrice.compareTo(contract.getStrikePrice()) <= 0) { // If spot price LTE the strike price
          callGexPerStrike.merge(contract.getStrikePrice(), optionContractGEX.getCallGEX(), BigDecimal::add);
        }
        totalGEX.setTotalCallGEX(totalGEX.getTotalCallGEX().add(optionContractGEX.getCallGEX()));
      }
      optionContractGEX.setTotalGEX(optionContractGEX.getCallGEX().add(optionContractGEX.getPutGEX()));
    });
    // Calculate total GEX
    totalGEX.setTotalGEX(totalGEX.getTotalCallGEX().add(totalGEX.getTotalPutGEX()));
    // Find CALL wall
    callGexPerStrike.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
        .ifPresent(totalGEX::setCallWall);
    // Find PUT wall
    putGexPerStrike.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
        .ifPresent(totalGEX::setPutWall);
    return totalGEX;
  }

}
