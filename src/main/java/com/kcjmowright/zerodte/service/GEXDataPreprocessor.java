package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.TotalGEX;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GEXDataPreprocessor {

  @Getter
  private final NormalizerMinMaxScaler featureScaler = new NormalizerMinMaxScaler(0, 1);
  @Getter
  private final NormalizerMinMaxScaler targetScaler = new NormalizerMinMaxScaler(0, 1);
  private final Map<String, Integer> featureIndices = new HashMap<>();

  public GEXDataPreprocessor() {
    initializeFeatureIndices();
  }

  private void initializeFeatureIndices() {
    int idx = 0;

    // Core GEX features
    featureIndices.put("distanceToCallWall", idx++);
    featureIndices.put("distanceToPutWall", idx++);
    featureIndices.put("distanceToFlipPoint", idx++);
    featureIndices.put("callPutGEXRatio", idx++);
    featureIndices.put("netGEX", idx++);
    featureIndices.put("gexSkew", idx++);
    featureIndices.put("concentrationIndex", idx++);
    featureIndices.put("relativePosition", idx++);

    // Time features
    featureIndices.put("minutesToExpiry", idx++);
    featureIndices.put("hourOfDay", idx++);
    featureIndices.put("minuteOfHour", idx++);

    // Price dynamics
    featureIndices.put("priceVelocity", idx++);
    featureIndices.put("priceAcceleration", idx++);
    featureIndices.put("volatility5min", idx++);
    featureIndices.put("volatility15min", idx++);

    // Technical indicators
    featureIndices.put("rsi", idx++);
    featureIndices.put("macdSignal", idx++);

    // Volume/liquidity
    featureIndices.put("relativeVolume", idx++);

    // Regime indicators
    featureIndices.put("isPositiveGEX", idx++);
    featureIndices.put("gexRegimeStrength", idx);
  }

  /**
   * Convert GEX snapshots into training dataset
   */
  public DataSet createDataSet(List<TotalGEX> snapshots,
                               List<GEXFeatures> features,
                               int predictionHorizonMinutes) {

    int numSamples = snapshots.size() - predictionHorizonMinutes;
    int numFeatures = featureIndices.size();

    INDArray featureMatrix = Nd4j.create(numSamples, numFeatures);
    INDArray labelVector = Nd4j.create(numSamples, 1);

    for (int i = 0; i < numSamples; i++) {
      TotalGEX currentSnapshot = snapshots.get(i);
      GEXFeatures currentFeatures = features.get(i);
      TotalGEX futureSnapshot = snapshots.get(i + predictionHorizonMinutes);

      // Extract features
      double[] featureVector = extractFeatureVector(currentSnapshot, currentFeatures);
      featureMatrix.putRow(i, Nd4j.create(featureVector));

      // Target: percentage price change
      double priceChange = calculatePriceChange(
          currentSnapshot.getSpotPrice(),
          futureSnapshot.getSpotPrice()
      );
      labelVector.putScalar(i, 0, priceChange);
    }

    DataSet dataSet = new DataSet(featureMatrix, labelVector);

    // Fit scalers on training data
    featureScaler.fit(dataSet);
    targetScaler.fit(dataSet);

    // Transform data
    featureScaler.transform(dataSet);
    targetScaler.transform(dataSet);

    return dataSet;
  }

  /**
   * Create time series dataset for LSTM
   */
  public DataSet createTimeSeriesDataSet(List<TotalGEX> snapshots,
                                         List<GEXFeatures> features,
                                         int sequenceLength,
                                         int predictionHorizon) {

    int numSamples = snapshots.size() - sequenceLength - predictionHorizon;
    int numFeatures = featureIndices.size();

    // [samples, features, timeSteps]
    INDArray featureTensor = Nd4j.create(numSamples, numFeatures, sequenceLength);
    INDArray labelVector = Nd4j.create(numSamples, 1);

    for (int i = 0; i < numSamples; i++) {
      // Get sequence of features
      for (int t = 0; t < sequenceLength; t++) {
        TotalGEX snapshot = snapshots.get(i + t);
        GEXFeatures feature = features.get(i + t);
        double[] featureVector = extractFeatureVector(snapshot, feature);

        for (int f = 0; f < numFeatures; f++) {
          featureTensor.putScalar(new int[]{i, f, t}, featureVector[f]);
        }
      }

      // Target is price change after sequence
      TotalGEX currentSnapshot = snapshots.get(i + sequenceLength - 1);
      TotalGEX futureSnapshot = snapshots.get(i + sequenceLength + predictionHorizon);

      double priceChange = calculatePriceChange(
          currentSnapshot.getSpotPrice(),
          futureSnapshot.getSpotPrice()
      );
      labelVector.putScalar(i, 0, priceChange);
    }

    DataSet dataSet = new DataSet(featureTensor, labelVector);

    // Normalize
    featureScaler.fit(dataSet);
    targetScaler.fit(dataSet);
    featureScaler.transform(dataSet);
    targetScaler.transform(dataSet);

    return dataSet;
  }

  private double[] extractFeatureVector(TotalGEX snapshot, GEXFeatures features) {
    double[] vector = new double[featureIndices.size()];

    // Core GEX features
    vector[featureIndices.get("distanceToCallWall")] =
        features.getDistanceToCallWall().doubleValue();
    vector[featureIndices.get("distanceToPutWall")] =
        features.getDistanceToPutWall().doubleValue();
    vector[featureIndices.get("distanceToFlipPoint")] =
        features.getDistanceToFlipPoint().doubleValue();
    vector[featureIndices.get("callPutGEXRatio")] =
        features.getCallPutGEXRatio().doubleValue();
    vector[featureIndices.get("netGEX")] =
        features.getNetGEX().doubleValue();
    vector[featureIndices.get("gexSkew")] =
        features.getGexSkew().doubleValue();
    vector[featureIndices.get("concentrationIndex")] =
        features.getConcentrationIndex().doubleValue();
    vector[featureIndices.get("relativePosition")] =
        features.getRelativePosition().doubleValue();

    // Time features
    vector[featureIndices.get("minutesToExpiry")] =
        features.getMinutesToExpiry().doubleValue();
    vector[featureIndices.get("hourOfDay")] =
        snapshot.getTimestamp().getHour();
    vector[featureIndices.get("minuteOfHour")] =
        snapshot.getTimestamp().getMinute();

    // Price dynamics
    vector[featureIndices.get("priceVelocity")] =
        features.getPriceVelocity().doubleValue();
    vector[featureIndices.get("priceAcceleration")] = 0.0; // Calculated separately
    vector[featureIndices.get("volatility5min")] = 0.0; // Calculated separately
    vector[featureIndices.get("volatility15min")] = 0.0; // Calculated separately

    // Technical indicators (placeholders)
    vector[featureIndices.get("rsi")] = 50.0;
    vector[featureIndices.get("macdSignal")] = 0.0;
    vector[featureIndices.get("relativeVolume")] = 1.0;

    // Regime indicators
    vector[featureIndices.get("isPositiveGEX")] =
        snapshot.getSpotPrice().compareTo(snapshot.getFlipPoint()) > 0 ? 1.0 : 0.0;
    vector[featureIndices.get("gexRegimeStrength")] =
        Math.abs(features.getDistanceToFlipPoint().doubleValue());

    return vector;
  }

  private double calculatePriceChange(BigDecimal currentPrice, BigDecimal futurePrice) {
    return futurePrice.subtract(currentPrice)
        .divide(currentPrice, 6, java.math.RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .doubleValue();
  }

  /**
   * Inverse transform predictions back to original scale
   */
  public double denormalizePrediction(double normalizedValue) {
    INDArray normalized = Nd4j.scalar(normalizedValue);
    DataSet dataSet = new DataSet(null, normalized);
    targetScaler.revert(dataSet);
    INDArray denormalized = dataSet.getLabels();
    return denormalized.getDouble(0);
  }

  /**
   * Split data into train/validation/test sets
   */
  public Map<String, DataSet> splitDataSet(DataSet fullDataSet,
                                           double trainRatio,
                                           double validationRatio) {

    int numSamples = fullDataSet.numExamples();
    int trainSize = (int) (numSamples * trainRatio);
    int validSize = (int) (numSamples * validationRatio);

    List<DataSet> splits = fullDataSet.asList();

    DataSet trainSet = DataSet.merge(splits.subList(0, trainSize));
    DataSet validSet = DataSet.merge(splits.subList(trainSize, trainSize + validSize));
    DataSet testSet = DataSet.merge(splits.subList(trainSize + validSize, numSamples));

    Map<String, DataSet> result = new HashMap<>();
    result.put("train", trainSet);
    result.put("validation", validSet);
    result.put("test", testSet);

    return result;
  }

  public int getNumFeatures() {
    return featureIndices.size();
  }

}
