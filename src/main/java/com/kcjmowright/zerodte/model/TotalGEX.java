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

import static java.util.Objects.nonNull;

@Getter
@Setter
@NoArgsConstructor
public class TotalGEX {
  private Map<ExpirationDateStrike, OptionContractGEX> optionContractGEXMap = new TreeMap<>(Comparator.naturalOrder());
  private BigDecimal totalCallGEX = BigDecimal.ZERO;
  private BigDecimal totalPutGEX = BigDecimal.ZERO;
  private BigDecimal totalGEX = BigDecimal.ZERO;

  /**
   *
   * @param optionContracts a {@link Stream} of {@link OptionContract} elements.
   * @param spotPrice current underlying price
   * @return total GEX computations
   */
  public static TotalGEX fromOptionContracts(
      @NonNull Stream<OptionContract> optionContracts,
      @NonNull BigDecimal spotPrice) {

    final TotalGEX totalGEX = new TotalGEX();
    optionContracts.forEach(contract -> {
      ExpirationDateStrike key = new ExpirationDateStrike(contract.getExpirationDate(), contract.getStrikePrice());
      OptionContractGEX optionContractGEX =
          totalGEX.getOptionContractGEXMap().computeIfAbsent(key, $ -> new OptionContractGEX(key));
      if (contract.getPutCall() == OptionContract.PutCall.PUT) {
        optionContractGEX.setPutContract(contract);
        optionContractGEX.setPutGEX(GammaExposure.putGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice));
        totalGEX.setTotalPutGEX(totalGEX.getTotalPutGEX().add(optionContractGEX.getPutGEX()));
      } else {
        optionContractGEX.setCallContract(contract);
        optionContractGEX.setCallGEX(GammaExposure.callGEX(contract.getGamma(), contract.getOpenInterest(), spotPrice));
        totalGEX.setTotalCallGEX(totalGEX.getTotalCallGEX().add(optionContractGEX.getCallGEX()));
      }
      if (nonNull(optionContractGEX.getCallGEX()) && nonNull(optionContractGEX.getPutContract())) {
        BigDecimal totalContractGEX = optionContractGEX.getCallGEX().add(optionContractGEX.getPutGEX());
        optionContractGEX.setTotalGEX(totalContractGEX);
        totalGEX.setTotalGEX(totalGEX.getTotalGEX().add(totalContractGEX));
      }
    });
    return totalGEX;
  }

}
