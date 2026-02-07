package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.TotalGEX;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GEXDataPreprocessor {

  @Value("${zerodte.model.basePath:./data/}")
  private String basePath;
  @Getter
  private NormalizerMinMaxScaler featureScaler = new NormalizerMinMaxScaler(0, 1);
  @Getter
  private NormalizerMinMaxScaler targetScaler = new NormalizerMinMaxScaler(0, 1);
  @Getter
  private boolean scalersFitted = false;
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
//    featureIndices.put("volatility5min", idx++);
//    featureIndices.put("volatility15min", idx++);

    // Technical indicators
    featureIndices.put("cci", idx++);
    featureIndices.put("stochastic", idx++);

    // Volume/liquidity
    featureIndices.put("relativeVolume", idx++);

    // Regime indicators
    featureIndices.put("isPositiveGEX", idx++);
    featureIndices.put("gexRegimeStrength", idx++);

    featureIndices.put("vix", idx);
  }

  /**
   * Convert GEX snapshots into training dataset
   */
  public DataSet createDataSet(List<GEXData> snapshots,
                               List<GEXFeatures> features,
                               int predictionHorizonMinutes) {

    int numSamples = snapshots.size() - predictionHorizonMinutes;
    int numFeatures = featureIndices.size();

    INDArray featureMatrix = Nd4j.create(numSamples, numFeatures);
    INDArray labelVector = Nd4j.create(numSamples, 1);

    for (int i = 0; i < numSamples; i++) {
      GEXData currentGEXData = snapshots.get(i);
      TotalGEX currentSnapshot = currentGEXData.getTotalGEX();
      GEXFeatures currentFeatures = features.get(i);
      TotalGEX futureSnapshot = snapshots.get(i + predictionHorizonMinutes).getTotalGEX();

      // Extract features
      double[] featureVector = extractFeatureVector(currentGEXData, currentFeatures);
      featureMatrix.putRow(i, Nd4j.create(featureVector));

      // Target: percentage price change
      double priceChange = calculatePriceChange(
          currentSnapshot.getSpotPrice(),
          futureSnapshot.getSpotPrice()
      );

      labelVector.putScalar(new int[]{i, 0}, priceChange);
    }

    DataSet dataSet = new DataSet(featureMatrix, labelVector);

    // Fit scalers on training data
    featureScaler.fit(dataSet);
    targetScaler.fit(dataSet);

    // Transform data
    featureScaler.transform(dataSet);
    targetScaler.transform(dataSet);

    scalersFitted = true;

    return dataSet;
  }

  /**
   * Create time series dataset for LSTM - Using 3D labels with masking
   */
  public DataSet createTimeSeriesDataSet(List<GEXData> snapshots,
                                         List<GEXFeatures> features,
                                         int sequenceLength,
                                         int predictionHorizon) {

    int numSamples = snapshots.size() - sequenceLength - predictionHorizon;
    int numFeatures = featureIndices.size();

    // Features: [samples, features, timeSteps]
    INDArray featureTensor = Nd4j.create(numSamples, numFeatures, sequenceLength);

    // FIX: Create 3D labels matching RNN output [samples, outputs, timeSteps]
    // But only the LAST time step has the actual label (others are masked)
    INDArray labelTensor = Nd4j.create(numSamples, 1, sequenceLength);

    // Create mask: only last time step is used
    INDArray labelMask = Nd4j.zeros(numSamples, sequenceLength);

    for (int i = 0; i < numSamples; i++) {
      // Get sequence of features
      for (int t = 0; t < sequenceLength; t++) {
        GEXData snapshot = snapshots.get(i + t);
        GEXFeatures feature = features.get(i + t);
        double[] featureVector = extractFeatureVector(snapshot, feature);

        for (int f = 0; f < numFeatures; f++) {
          featureTensor.putScalar(new int[]{ i, f, t }, featureVector[f]);
        }
      }

      // Target is price change after sequence
      GEXData currentSnapshot = snapshots.get(i + sequenceLength - 1);
      GEXData futureSnapshot = snapshots.get(i + sequenceLength + predictionHorizon);

      double priceChange = calculatePriceChange(
          currentSnapshot.getTotalGEX().getSpotPrice(),
          futureSnapshot.getTotalGEX().getSpotPrice()
      );

      // Only set label for LAST time step
      labelTensor.putScalar(new int[]{ i, 0, sequenceLength - 1 }, priceChange);

      // Only last time step is active in mask
      labelMask.putScalar(new int[]{ i, sequenceLength - 1 }, 1.0);
    }

    DataSet dataSet = new DataSet(featureTensor, labelTensor, null, labelMask);

    // Normalize
    featureScaler.fit(dataSet);
    targetScaler.fit(dataSet);
    featureScaler.transform(dataSet);
    targetScaler.transform(dataSet);
    scalersFitted = true;
    return dataSet;
  }

  public double[] extractFeatureVector(GEXData GEXData, GEXFeatures features) {
    TotalGEX snapshot = GEXData.getTotalGEX();
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
    vector[featureIndices.get("priceVelocity")] = features.getPriceVelocity().doubleValue();
    vector[featureIndices.get("priceAcceleration")] = features.getPriceAcceleration().doubleValue();

    // Technical indicators
    vector[featureIndices.get("cci")] = features.getCci().doubleValue();
    vector[featureIndices.get("stochastic")] = features.getStochastic().doubleValue();
    vector[featureIndices.get("relativeVolume")] = 1.0;

    // Regime indicators
    vector[featureIndices.get("isPositiveGEX")] =
        snapshot.getSpotPrice().compareTo(snapshot.getFlipPoint()) > 0 ? 1.0 : 0.0;
    vector[featureIndices.get("gexRegimeStrength")] =
        Math.abs(features.getDistanceToFlipPoint().doubleValue());

    vector[featureIndices.get("vix")] = GEXData.getVix().doubleValue();
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
    if (!scalersFitted) {
      throw new IllegalStateException("Scalers must be fitted first");
    }

    // Create a 2D array for the label
    INDArray normalizedArray = Nd4j.create(new double[][]{{ normalizedValue }});

    // Create dataset with dummy features (same shape as training)
    // The targetScaler only operates on labels, but needs a valid DataSet
    INDArray dummyFeatures = Nd4j.create(1, 1);
    DataSet tempDataset = new DataSet(dummyFeatures, normalizedArray);

    // Revert modifies in-place
    targetScaler.revert(tempDataset);

    // Extract the denormalized value
    return tempDataset.getLabels().getDouble(0, 0);
  }

  /**
   * Inverse transform feature array back to original scale
   */
  public INDArray denormalizeFeatures(INDArray normalizedFeatures) {
    if (!scalersFitted) {
      throw new IllegalStateException("Scalers must be fitted first");
    }

    // Clone to avoid modifying original
    INDArray cloned = normalizedFeatures.dup();
    INDArray dummyLabels = Nd4j.create(cloned.shape()[0], 1);
    DataSet tempDataset = new DataSet(cloned, dummyLabels);
    featureScaler.revert(tempDataset);
    return tempDataset.getFeatures();
  }

  /**
   * Transform (normalize) a single prediction without fitting
   */
  public double normalizePrediction(double rawValue) {
    if (!scalersFitted) {
      throw new IllegalStateException("Scalers must be fitted first");
    }

    INDArray rawArray = Nd4j.create(new double[][]{{ rawValue }});
    INDArray dummyFeatures = Nd4j.create(1, 1);
    DataSet tempDataset = new DataSet(dummyFeatures, rawArray);
    targetScaler.transform(tempDataset);
    return tempDataset.getLabels().getDouble(0, 0);
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

  /**
   * Save scalers to disk
   */
  public void saveScalers() {
    if (!scalersFitted) {
      throw new IllegalStateException("Scalers must be fitted before saving");
    }

    File featureFile = new File(basePath + "_feature_scaler.bin");
    File targetFile = new File(basePath + "_target_scaler.bin");
    try {
      NormalizerSerializer.getDefault().write(featureScaler, featureFile);
      NormalizerSerializer.getDefault().write(targetScaler, targetFile);
      log.debug("Scalers saved to {} and {}", featureFile, targetFile);
      log.info("Scalars successfully saved");
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save scalars due to : %s".formatted(e.getMessage()), e);
    }
  }

  /**
   * Load scalers from disk
   */
  public void loadScalers() {
    File featureFile = new File(basePath + "_feature_scaler.bin");
    File targetFile = new File(basePath + "_target_scaler.bin");

    if (!featureFile.exists() || !targetFile.exists()) {
      throw new IllegalStateException("Scaler files not found at: " + basePath);
    }

    try {
      featureScaler = NormalizerSerializer.getDefault().restore(featureFile);
      targetScaler = NormalizerSerializer.getDefault().restore(targetFile);
      scalersFitted = true;
      log.debug("Scalers loaded from {} and {}", featureFile, targetFile);
      log.info("Scalars successfully loaded");
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load scalars due to: %s".formatted(e.getMessage()), e);
    }
  }

  /**
   * Save trained model to disk
   */
  public void saveModel(MultiLayerNetwork model) {
    File modelFile = new File(basePath + "model.bin");
    try {
      ModelSerializer.writeModel(model, modelFile, true);
      log.debug("Model saved to {}", modelFile.getAbsolutePath());
      log.info("Model saved successfully");
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save model due to %s".formatted(e.getMessage()), e);
    }
  }

  /**
   * Initialize predictor with trained model
   */
  public MultiLayerNetwork loadModel() {
    File file = new File(basePath + "model.bin");
    try {
      MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(file);
      log.debug("Model loaded from {}", file.getAbsolutePath());
      log.info("Successfully loaded model");
      return model;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load model data due to %s".formatted(e.getMessage()), e);
    }
  }

}
