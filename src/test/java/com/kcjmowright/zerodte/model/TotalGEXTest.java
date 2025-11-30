package com.kcjmowright.zerodte.model;

import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TotalGEXTest {

  @Test
  void shouldComputeTotalGEX() {
    try (Stream<OptionContract> contracts = Stream.of()) {
      TotalGEX totalGEX = TotalGEX.fromOptionContracts(contracts, new BigDecimal("100.0"), true);
      assertNotNull(totalGEX);
    }

  }
}
