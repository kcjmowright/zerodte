package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// Supporting classes
@Data
@Builder(toBuilder = true)
public class TrainingConfig {
  private String symbol;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int predictionHorizon;
  private String modelType;
  private int numEpochs;
  private int batchSize;
  private double learningRate;
  private double l2Regularization;
  private int seed;
  private double trainRatio;
  private double validationRatio;
  private int earlyStoppingPatience;
  private boolean useLearningRateDecay;
  private boolean useTimeSeries;
  private int sequenceLength;
  private String modelSavePath;
  private int numSamples;
}
