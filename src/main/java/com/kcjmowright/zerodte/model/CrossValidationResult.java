package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CrossValidationResult {
  private List<Double> foldScores;
  private double meanScore;
  private double stdScore;
}