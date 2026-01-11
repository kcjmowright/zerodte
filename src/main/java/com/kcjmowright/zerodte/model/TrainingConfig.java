package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TrainingConfig {
  private String symbol;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private Integer predictionHorizon;
  private String modelType;
  private Integer numEpochs;
  private Integer batchSize;
  private Double learningRate;
  private Double l2Regularization;
  private Integer seed;
  private Double trainRatio;
  private Double validationRatio;
  private Integer earlyStoppingPatience;
  private Boolean useLearningRateDecay = Boolean.FALSE;
  private Boolean useTimeSeries = Boolean.FALSE;
  private Integer sequenceLength;
  private Integer numSamples;
}
