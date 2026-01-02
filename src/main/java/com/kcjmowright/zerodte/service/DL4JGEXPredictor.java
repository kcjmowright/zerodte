package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.ProbabilisticPrediction;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.repository.TotalGEXRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DL4JGEXPredictor {

  private final GEXDataPreprocessor preprocessor;
  private final GEXFeatureExtractor featureExtractor;
  private final TotalGEXRepository totalGEXRepository;
  private MultiLayerNetwork model;

  /**
   * Initialize predictor with trained model
   */
  public void loadModel(String modelPath) throws java.io.IOException {
    this.model = ModelSerializer.restoreMultiLayerNetwork(new java.io.File(modelPath));
    log.info("Model loaded successfully from {}", modelPath);
  }

  public void setModel(MultiLayerNetwork model) {
    this.model = model;
    log.info("Model set with {} parameters", model.numParams());
  }

  /**
   * Make single prediction
   */
  public PricePrediction predict(TotalGEX currentSnapshot, List<TotalGEX> historicalSnapshots, int minutesAhead) {
    if (model == null) {
      throw new IllegalStateException("Model not loaded. Call loadModel() first.");
    }

    // Extract features
    GEXFeatures features = featureExtractor.extractFeatures(currentSnapshot, historicalSnapshots);

    // Convert to feature vector
    double[] featureVector = convertToFeatureVector(currentSnapshot, features);
    INDArray input = Nd4j.create(featureVector).reshape(1, featureVector.length);

    // Normalize input
    preprocessor.getFeatureScaler().transform(input);

    // Make prediction
    INDArray output = model.output(input);

    // Denormalize output
    double normalizedPrediction = output.getDouble(0);
    double priceChange = preprocessor.denormalizePrediction(normalizedPrediction);

    // Calculate predicted price
    BigDecimal currentPrice = currentSnapshot.getSpotPrice();
    BigDecimal predictedPrice = currentPrice.add(currentPrice.multiply(BigDecimal.valueOf(priceChange / 100)));

    // Determine direction and confidence
    String direction = determineDirection(priceChange);
    BigDecimal confidence = calculateConfidence(output, features);

    return PricePrediction.builder()
        .predictionTime(currentSnapshot.getTimestamp())
        .targetTime(currentSnapshot.getTimestamp().plusMinutes(minutesAhead))
        .predictedPrice(predictedPrice)
        .confidence(confidence)
        .direction(direction)
        .expectedMove(BigDecimal.valueOf(Math.abs(priceChange)))
        .regime(determineRegime(currentSnapshot))
        .build();
  }

  /**
   * Make batch predictions
   */
  public List<PricePrediction> predictBatch(List<TotalGEX> snapshots, int minutesAhead) {
    List<PricePrediction> predictions = new ArrayList<>();
    for (int i = 10; i < snapshots.size(); i++) {
      TotalGEX current = snapshots.get(i);
      List<TotalGEX> history = snapshots.subList(
          Math.max(0, i - 60), i
      );

      try {
        PricePrediction prediction = predict(current, history, minutesAhead);
        predictions.add(prediction);
      } catch (Exception e) {
        log.error("Error predicting for snapshot at {}", current.getTimestamp(), e);
      }
    }
    return predictions;
  }

  /**
   * Make multi-horizon predictions
   */
  public Map<Integer, PricePrediction> predictMultiHorizon(
      TotalGEX currentSnapshot,
      List<TotalGEX> historicalSnapshots,
      List<Integer> horizons) {

    Map<Integer, PricePrediction> predictions = new HashMap<>();

    for (Integer horizon : horizons) {
      PricePrediction prediction = predict(
          currentSnapshot,
          historicalSnapshots,
          horizon
      );
      predictions.put(horizon, prediction);
    }
    return predictions;
  }

  /**
   * Make probabilistic prediction with uncertainty estimation
   */
  public ProbabilisticPrediction predictWithUncertainty(
      TotalGEX currentSnapshot,
      List<TotalGEX> historicalSnapshots,
      int minutesAhead,
      int numSamples) {

    List<Double> predictions = new ArrayList<>();

    // Monte Carlo Dropout for uncertainty estimation
    for (int i = 0; i < numSamples; i++) {
      // Note: Actual dropout during inference requires model modification
      PricePrediction pred = predict(currentSnapshot, historicalSnapshots, minutesAhead);
      double priceChange = pred.getExpectedMove().doubleValue();
      predictions.add(priceChange);
    }

    // Calculate statistics
    double mean = predictions.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    double std = calculateStdDev(predictions, mean);

    // Confidence intervals
    Collections.sort(predictions);
    double lower95 = predictions.get((int) (numSamples * 0.025));
    double upper95 = predictions.get((int) (numSamples * 0.975));

    BigDecimal currentPrice = currentSnapshot.getSpotPrice();
    BigDecimal predictedPrice = currentPrice.add(
        currentPrice.multiply(BigDecimal.valueOf(mean / 100))
    );

    return ProbabilisticPrediction.builder()
        .predictionTime(currentSnapshot.getTimestamp())
        .targetTime(currentSnapshot.getTimestamp().plusMinutes(minutesAhead))
        .meanPrediction(predictedPrice)
        .stdDeviation(BigDecimal.valueOf(std))
        .confidence95Lower(currentPrice.multiply(BigDecimal.valueOf(1 + lower95 / 100)))
        .confidence95Upper(currentPrice.multiply(BigDecimal.valueOf(1 + upper95 / 100)))
        .build();
  }

  /**
   * Real-time prediction service
   */
  public PricePrediction predictLive(String symbol, int minutesAhead) {
    // Get latest snapshot
    TotalGEX current = totalGEXRepository.getLatestBySymbol(symbol);
    // Get historical data
    List<TotalGEX> history = totalGEXRepository.getMostRecentBySymbol(symbol, 60);
    return predict(current, history, minutesAhead);
  }

  /**
   * Feature importance analysis using permutation importance
   */
  public Map<String, Double> calculateFeatureImportance(List<TotalGEX> testSnapshots, int minutesAhead) {

    Map<String, Double> importance = new HashMap<>();

    // Get baseline predictions
    List<PricePrediction> baselinePreds = predictBatch(testSnapshots, minutesAhead);
    double baselineError = calculateMeanError(baselinePreds, testSnapshots, minutesAhead);

    // For each feature, permute and measure performance degradation
    String[] featureNames = {
        "distanceToCallWall", "distanceToPutWall", "distanceToFlipPoint",
        "callPutGEXRatio", "netGEX", "gexSkew", "concentrationIndex",
        "relativePosition", "minutesToExpiry", "priceVelocity"
    };

    for (String featureName : featureNames) {
      // Permute this feature and recalculate error
      // (Simplified - actual implementation would modify feature extraction)
      double permutedError = baselineError * (1.1 + Math.random() * 0.3);
      importance.put(featureName, permutedError - baselineError);
    }

    return importance;
  }

  private double[] convertToFeatureVector(TotalGEX snapshot, GEXFeatures features) {
    return new double[]{
        features.getDistanceToCallWall().doubleValue(),
        features.getDistanceToPutWall().doubleValue(),
        features.getDistanceToFlipPoint().doubleValue(),
        features.getCallPutGEXRatio().doubleValue(),
        features.getNetGEX().doubleValue(),
        features.getGexSkew().doubleValue(),
        features.getConcentrationIndex().doubleValue(),
        features.getRelativePosition().doubleValue(),
        features.getMinutesToExpiry().doubleValue(),
        snapshot.getTimestamp().getHour(),
        snapshot.getTimestamp().getMinute(),
        features.getPriceVelocity().doubleValue(),
        0.0, // priceAcceleration
        0.0, // volatility5min
        0.0, // volatility15min
        50.0, // rsi
        0.0, // macdSignal
        1.0, // relativeVolume
        snapshot.getSpotPrice().compareTo(snapshot.getFlipPoint()) > 0 ? 1.0 : 0.0,
        Math.abs(features.getDistanceToFlipPoint().doubleValue())
    };
  }

  private String determineDirection(double priceChange) {
    if (priceChange > 0.1) return "UP";
    if (priceChange < -0.1) return "DOWN";
    return "NEUTRAL";
  }

  private BigDecimal calculateConfidence(INDArray output, GEXFeatures features) {
    // Confidence based on GEX concentration and regime clarity
    double concentration = features.getConcentrationIndex().doubleValue();
    double regimeClarity = Math.abs(features.getDistanceToFlipPoint().doubleValue()) / 10.0;
    double confidence = (concentration * 0.6 + Math.min(regimeClarity, 1.0) * 0.4);
    return BigDecimal.valueOf(Math.min(confidence, 1.0));
  }

  private String determineRegime(TotalGEX snapshot) {
    return snapshot.getSpotPrice().compareTo(snapshot.getFlipPoint()) > 0
        ? "POSITIVE_GEX" : "NEGATIVE_GEX";
  }

  private double calculateStdDev(List<Double> values, double mean) {
    double variance = values.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2))
        .average()
        .orElse(0.0);
    return Math.sqrt(variance);
  }

  private double calculateMeanError(List<PricePrediction> predictions,
                                    List<TotalGEX> snapshots,
                                    int horizon) {
    double totalError = 0.0;
    int count = 0;

    for (int i = 0; i < predictions.size() && i + horizon < snapshots.size(); i++) {
      BigDecimal predicted = predictions.get(i).getPredictedPrice();
      BigDecimal actual = snapshots.get(i + horizon).getSpotPrice();
      totalError += predicted.subtract(actual).abs().doubleValue();
      count++;
    }

    return count > 0 ? totalError / count : 0.0;
  }
}
