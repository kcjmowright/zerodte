package com.kcjmowright.zerodte.model;

import lombok.Data;

import java.util.List;

@Data
public class QuoteStudyResponse {
  private String symbol;
  private List<QuoteStudy> quoteStudies;
}
