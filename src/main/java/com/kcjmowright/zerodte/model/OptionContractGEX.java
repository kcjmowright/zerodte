package com.kcjmowright.zerodte.model;

import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class OptionContractGEX implements Comparable<OptionContractGEX> {
  private final BigDecimal strike;
  private final List<OptionContract> contracts = new ArrayList<>();
  private BigDecimal callGEX = BigDecimal.ZERO;
  private BigDecimal putGEX = BigDecimal.ZERO;
  private BigDecimal totalGEX = BigDecimal.ZERO;
  private BigDecimal absoluteGEX = BigDecimal.ZERO;
  private BigDecimal openInterest =  BigDecimal.ZERO;

  @Override
  public int compareTo(OptionContractGEX o) {
    return strike.compareTo(o.getStrike());
  }
}
