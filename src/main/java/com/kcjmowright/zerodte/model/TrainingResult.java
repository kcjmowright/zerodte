package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
