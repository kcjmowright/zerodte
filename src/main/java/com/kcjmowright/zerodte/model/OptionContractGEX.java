package com.kcjmowright.zerodte.model;

import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class OptionContractGEX {
  private final ExpirationDateStrike expirationDateStrike;
  private OptionContract callContract;
  private OptionContract putContract;
  private BigDecimal callGEX;
  private BigDecimal putGEX;
  private BigDecimal totalGEX;
}
