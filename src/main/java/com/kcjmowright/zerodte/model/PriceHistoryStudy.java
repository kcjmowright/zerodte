package com.kcjmowright.zerodte.model;

import com.pangility.schwab.api.client.marketdata.model.pricehistory.Candle;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class PriceHistoryStudy {
  private Candle candle;
  private Map<String, BigDecimal> studies = new HashMap<>();
}
