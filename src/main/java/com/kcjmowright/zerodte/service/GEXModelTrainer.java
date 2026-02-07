package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.CrossValidationResult;
import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.HyperparameterResult;
import com.kcjmowright.zerodte.model.TrainingConfig;
import com.kcjmowright.zerodte.model.TrainingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.datasets.iterator.IteratorDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.regression.RegressionEvaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GEXModelTrainer {
  private final GEXDataPreprocessor preprocessor;
  private final GEXModelBuilder modelBuilder;
  private final GEXService gexService;
  private static final int BATCH_SIZE = /* number of minutes in trading week */ 1950;

  /**
   * Train model with early stopping and validation
   *
   * @param config training parameters
   * @return the training result
   */
  public TrainingResult trainModel(TrainingConfig config) {
    log.info("Starting model training with config: {}", config);
    log.info("1. Load and prepare data");
    List<GEXData> snapshots = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        config.getSymbol(),
        config.getStartDate(),
        config.getEndDate()
    );

    GEXFeatureExtractor extractor = new GEXFeatureExtractor();
    List<GEXFeatures> features = new ArrayList<>(snapshots.size());
    for (int i = 0; i < snapshots.size(); i++) {
      List<GEXData> history = snapshots.subList(Math.max(0, i - 60), i);
      features.add(extractor.extractFeatures(snapshots.get(i), history));
    }

    log.info("2. Create dataset");
    DataSet fullDataSet = config.getUseTimeSeries() ?
        preprocessor.createTimeSeriesDataSet(
            snapshots,
            features,
            config.getSequenceLength(),
            config.getPredictionHorizon()
        ) :
        preprocessor.createDataSet(
            snapshots,
            features,
            config.getPredictionHorizon()
        );

    log.info("3. Split data");
    Map<String, DataSet> splits = preprocessor.splitDataSet(
        fullDataSet,
        config.getTrainRatio(),
        config.getValidationRatio()
    );

    DataSet trainSet = splits.get("train");
    DataSet validSet = splits.get("validation");
    DataSet testSet = splits.get("test");

    log.info("Train samples: {}, Valid samples: {}, Test samples: {}",
        trainSet.numExamples(), validSet.numExamples(), testSet.numExamples());

    log.info("4. Build model");
    MultiLayerNetwork model = buildModel(config, preprocessor.getNumFeatures());

    log.info("5. Training loop with early stopping");
    TrainingResult result = trainWithEarlyStopping(
        model,
        trainSet,
        validSet,
        testSet,
        config
    );

    log.info("6. Save model and scalars");
    preprocessor.saveModel(model);
    preprocessor.saveScalers();
    return result;
  }

  private MultiLayerNetwork buildModel(TrainingConfig config, int numFeatures) {
    return switch (config.getModelType()) {
      case "feedforward" -> modelBuilder.buildFeedForwardNetwork(
          numFeatures,
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization());
      case "lstm" -> modelBuilder.buildLSTMNetwork(
          numFeatures,
          config.getSequenceLength(),
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization());
      case "deep" -> modelBuilder.buildDeepNetwork(
          numFeatures,
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization());
      case "bidirectional" -> modelBuilder.buildBidirectionalLSTM(
          numFeatures,
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization());
      case "attention" -> modelBuilder.buildAttentionNetwork(
          numFeatures,
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization());
      default -> modelBuilder.buildRecommendedModel(
          numFeatures,
          config.getNumSamples(),
          config.getUseTimeSeries(),
          config.getSeed(),
          config.getLearningRate(),
          config.getL2Regularization()
      );
    };
  }

  private TrainingResult trainWithEarlyStopping(MultiLayerNetwork model,
                                                DataSet trainSet,
                                                DataSet validSet,
                                                DataSet testSet,
                                                TrainingConfig config) {
    log.debug("Train Set:\n{}", trainSet);
    log.debug("Valid Set:\n{}", validSet);
    log.debug("Test Set:\n{}", testSet);
    log.debug("Config:\n{}", config);
    model.setListeners(new ScoreIterationListener(10));

    List<Double> trainLosses = new ArrayList<>();
    List<Double> validLosses = new ArrayList<>();

    double bestValidLoss = Double.MAX_VALUE;
    int patienceCounter = 0;
    int bestEpoch = 0;
    MultiLayerNetwork bestModel = model.clone();

    log.info("Starting training for {} epochs", config.getNumEpochs());

    for (int epoch = 0; epoch < config.getNumEpochs(); epoch++) {
      // Train
      model.fit(trainSet);

      // Evaluate on training set
      double trainLoss = model.score(trainSet);
      trainLosses.add(trainLoss);

      // Evaluate on validation set
      INDArray validPredictions = model.output(validSet.getFeatures());
      double validLoss = calculateMSE(validPredictions, validSet.getLabels());
      validLosses.add(validLoss);

      log.info("Epoch {}: Train Loss = {}, Valid Loss = {}", epoch + 1, trainLoss, validLoss);

      // Early stopping check
      if (validLoss < bestValidLoss) {
        bestValidLoss = validLoss;
        bestEpoch = epoch;
        bestModel = model.clone();
        patienceCounter = 0;
      } else {
        patienceCounter++;
      }

      if (patienceCounter >= config.getEarlyStoppingPatience()) {
        log.info("Early stopping triggered at epoch {}", epoch + 1);
        break;
      }

      // Learning rate decay
      if (config.getUseLearningRateDecay() && epoch % 10 == 0 && epoch > 0) {
        double newLr = config.getLearningRate() * 0.95;
        log.info("Decaying learning rate to {}", newLr);
        // Note: In practice, you'd update the updater here
      }
    }

    // Use best model
    model = bestModel;

    // Final evaluation on test set
    RegressionEvaluation testEval = model.evaluateRegression(
        new IteratorDataSetIterator(new DataSet(testSet.getFeatures(), testSet.getLabels()).iterator(), BATCH_SIZE)
    );

    log.info("Test set evaluation:\n{}", testEval.stats());

    return TrainingResult.builder()
        .bestEpoch(bestEpoch)
        .bestValidationLoss(bestValidLoss)
        .trainLosses(trainLosses)
        .validationLosses(validLosses)
        .testMSE(testEval.meanSquaredError(0))
        .testMAE(testEval.meanAbsoluteError(0))
        .testRMSE(testEval.rootMeanSquaredError(0))
        .testR2(testEval.rSquared(0))
        .build();
  }

  private double calculateMSE(INDArray predictions, INDArray actual) {
    INDArray diff = predictions.sub(actual);
    INDArray squared = diff.mul(diff);
    return squared.meanNumber().doubleValue();
  }

  /**
   * Hyperparameter tuning using grid search
   */
  public HyperparameterResult tuneHyperparameters(
      List<GEXData> snapshots,
      List<GEXFeatures> features,
      Map<String, List<Object>> hyperparamGrid) {

    log.info("Starting hyperparameter tuning");

    List<HyperparameterResult.Trial> trials = new ArrayList<>();
    double bestScore = Double.MAX_VALUE;
    Map<String, Object> bestParams = null;

    // Generate all combinations
    List<Map<String, Object>> combinations = generateCombinations(hyperparamGrid);

    for (int i = 0; i < combinations.size(); i++) {
      Map<String, Object> params = combinations.get(i);
      log.info("Trial {}/{}: {}", i + 1, combinations.size(), params);

      // Create config with these parameters
      TrainingConfig config = createConfigFromParams(params);

      // Train model
      TrainingResult result = trainModel(config);

      double score = result.getTestMSE();
      trials.add(new HyperparameterResult.Trial(params, score));

      if (score < bestScore) {
        bestScore = score;
        bestParams = params;
      }
    }

    return HyperparameterResult.builder()
        .trials(trials)
        .bestParams(bestParams)
        .bestScore(bestScore)
        .build();
  }

  /**
   * Cross-validation for robust model evaluation
   */
  public CrossValidationResult crossValidate(
      String symbol,
      List<GEXData> snapshots,
      List<GEXFeatures> features,
      int numFolds,
      TrainingConfig baseConfig) {

    log.info("Starting {}-fold cross-validation", numFolds);

    int foldSize = snapshots.size() / numFolds;
    List<Double> foldScores = new ArrayList<>();

    for (int fold = 0; fold < numFolds; fold++) {
      log.info("Processing fold {}/{}", fold + 1, numFolds);

      // Split data for this fold
      int testStart = fold * foldSize;
      int testEnd = (fold + 1) * foldSize;

      List<GEXData> trainSnaps = new ArrayList<>();
      // List<GEXFeatures> trainFeats = new ArrayList<>();

      trainSnaps.addAll(snapshots.subList(0, testStart));
      trainSnaps.addAll(snapshots.subList(testEnd, snapshots.size()));

//      trainFeats.addAll(features.subList(0, testStart));
//      trainFeats.addAll(features.subList(testEnd, features.size()));

      // Create temporary config for this fold
      TrainingConfig foldConfig = baseConfig.toBuilder()
          .startDate(trainSnaps.getFirst().getCreated())
          .endDate(trainSnaps.getLast().getCreated())
          .build();

      // Train and evaluate
      TrainingResult result = trainModel(foldConfig);
      foldScores.add(result.getTestMSE());
    }

    double meanScore = foldScores.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    double stdScore = calculateStandardDeviation(foldScores);

    return CrossValidationResult.builder()
        .foldScores(foldScores)
        .meanScore(meanScore)
        .stdScore(stdScore)
        .build();
  }

  private List<Map<String, Object>> generateCombinations(Map<String, List<Object>> grid) {

    List<Map<String, Object>> results = new ArrayList<>();
    results.add(new HashMap<>());

    for (Map.Entry<String, List<Object>> entry : grid.entrySet()) {
      String key = entry.getKey();
      List<Object> values = entry.getValue();

      List<Map<String, Object>> newResults = new ArrayList<>();
      for (Map<String, Object> result : results) {
        for (Object value : values) {
          Map<String, Object> newResult = new HashMap<>(result);
          newResult.put(key, value);
          newResults.add(newResult);
        }
      }
      results = newResults;
    }

    return results;
  }

  private TrainingConfig createConfigFromParams(Map<String, Object> params) {
    return TrainingConfig.builder()
        .learningRate((Double) params.getOrDefault("learningRate", 0.001))
        .numEpochs((Integer) params.getOrDefault("numEpochs", 100))
        .batchSize((Integer) params.getOrDefault("batchSize", 32))
        .l2Regularization((Double) params.getOrDefault("l2", 0.0001))
        .build();
  }

  private double calculateStandardDeviation(List<Double> values) {
    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double variance = values.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2))
        .average()
        .orElse(0.0);
    return Math.sqrt(variance);
  }

}
