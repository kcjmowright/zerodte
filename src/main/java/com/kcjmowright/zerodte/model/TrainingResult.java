package com.kcjmowright.zerodte.model;

import lombok.Builder;
import lombok.Data;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.util.List;

@Data
@Builder
public class TrainingResult {
  private MultiLayerNetwork model;
  private int bestEpoch;
  private double bestValidationLoss;
  private List<Double> trainLosses;
  private List<Double> validationLosses;
  private double testMSE;
  private double testMAE;
  private double testRMSE;
  private double testR2;
  private String modelPath;
}
