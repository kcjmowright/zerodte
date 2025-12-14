package com.kcjmowright.zerodte.model;

import lombok.Data;

import java.util.List;

@Data
public class PriceHistoryStudyResponse {
  private String symbol;
  private List<PriceHistoryStudy> priceHistoryStudies;
}
