package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HyperparameterResult {
  private List<Trial> trials;
  private Map<String, Object> bestParams;
  private double bestScore;

  @Data
  @AllArgsConstructor
  public static class Trial {
    private Map<String, Object> params;
    private double score;
  }
}
