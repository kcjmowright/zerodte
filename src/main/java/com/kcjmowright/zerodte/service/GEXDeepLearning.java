package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.CrossValidationResult;
import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.HyperparameterResult;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.ProbabilisticPrediction;
import com.kcjmowright.zerodte.model.TrainingConfig;
import com.kcjmowright.zerodte.model.TrainingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GEXDeepLearning {
  private final GEXModelTrainer trainer;
  private final GEXPredictor predictor;
  private final GEXDataPreprocessor preprocessor;
  private final GEXService gexService;

  public void run(String... args) {
    log.info("Starting GEX Deep Learning Example");
    String symbol = args[0];
    LocalDateTime startTime = LocalDateTime.parse(args[1]);
    LocalDateTime endTime = LocalDateTime.parse(args[2]);

    // Example 1: Basic model training
    basicTrainingExample(symbol, startTime, endTime);

    // Example 2: Hyperparameter tuning
    hyperparameterTuningExample(symbol, startTime, endTime);

    // Example 3: Cross-validation
    crossValidationExample(symbol, startTime, endTime);

    // Example 4: Live prediction
    livePredictionExample(symbol);

    modelComparisonExample(symbol, startTime, endTime);
  }

  /**
   * Example 1: Train a basic model
   */
  public void basicTrainingExample(String symbol, LocalDateTime start, LocalDateTime end) {
    log.info("=== Example 1: Basic Model Training ===");

    // 1. Configure training
    TrainingConfig config = TrainingConfig.builder()
        .symbol(symbol)
        .startDate(start)
        .endDate(end)
        .predictionHorizon(60) // Predict 60 minutes ahead
        .modelType("feedforward") // or "lstm", "deep", "attention"
        .numEpochs(50)
        .batchSize(32)
        .learningRate(0.001)
        .l2Regularization(0.0001)
        .seed(12345)
        .trainRatio(0.7)
        .validationRatio(0.15)
        .earlyStoppingPatience(10)
        .useLearningRateDecay(true)
        .useTimeSeries(false)
        .build();

    // 2. Train model
    log.info("Starting training with config: {}", config);
    TrainingResult result = trainer.trainModel(config);

    // 3. Print results
    log.info("Training completed!");
    log.info("Best epoch: {}", result.getBestEpoch());
    log.info("Best validation loss: {}", result.getBestValidationLoss());
    log.info("Test MSE: {}", result.getTestMSE());
    log.info("Test MAE: {}", result.getTestMAE());
    log.info("Test RMSE: {}", result.getTestRMSE());
    log.info("Test R²: {}", result.getTestR2());

    // 4. Visualize training progress
    visualizeTrainingProgress(result);

    // 5. Make predictions with trained model
    predictor.setModel(preprocessor.loadModel());

    List<GEXData> history = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
    );
    PricePrediction prediction = predictor.predict(history.getLast(), history, 60);

    log.info("Sample Prediction:");
    log.info("  Current Price: {}", history.getLast().getTotalGEX().getSpotPrice());
    log.info("  Predicted Price: {}", prediction.getPredictedPrice());
    log.info("  Direction: {}", prediction.getDirection());
    log.info("  Confidence: {}", prediction.getConfidence());
  }

  /**
   * Example 2: Hyperparameter tuning
   */
  public void hyperparameterTuningExample(String symbol, LocalDateTime start, LocalDateTime end) {
    log.info("=== Example 2: Hyperparameter Tuning ===");

    // Define hyperparameter search space
    Map<String, List<Object>> hyperparamGrid = new HashMap<>();
    hyperparamGrid.put("learningRate", List.of(0.0001, 0.001, 0.01));
    hyperparamGrid.put("l2", List.of(0.00001, 0.0001, 0.001));
    hyperparamGrid.put("batchSize", List.of(16, 32, 64));
    hyperparamGrid.put("numEpochs", List.of(30, 50, 100));

    // Load data
    List<GEXData> snapshots = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        start,
        end
    );
    List<GEXFeatures> features = extractAllFeatures(snapshots);

    // Run grid search
    HyperparameterResult tuningResult = trainer.tuneHyperparameters(
        snapshots,
        features,
        hyperparamGrid
    );

    log.info("Best hyperparameters: {}", tuningResult.getBestParams());
    log.info("Best score: {}", tuningResult.getBestScore());

    // Show top 5 configurations
    tuningResult.getTrials().stream()
        .sorted(Comparator.comparing(HyperparameterResult.Trial::getScore))
        .limit(5)
        .forEach(trial ->
            log.info("Params: {}, Score: {}", trial.getParams(), trial.getScore())
        );
  }

  /**
   * Example 3: Cross-validation
   */
  public void crossValidationExample(String symbol, LocalDateTime start, LocalDateTime end) {
    log.info("=== Example 3: Cross-Validation ===");
    List<GEXData> snapshots = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        start,
        end
    );
    List<GEXFeatures> features = extractAllFeatures(snapshots);
    TrainingConfig baseConfig = TrainingConfig.builder()
        .symbol(symbol)
        .predictionHorizon(60)
        .modelType("feedforward")
        .numEpochs(30)
        .batchSize(32)
        .learningRate(0.001)
        .l2Regularization(0.0001)
        .seed(12345)
        .build();

    CrossValidationResult cvResult = trainer.crossValidate(
        symbol,
        snapshots,
        features,
        5, // 5-fold CV
        baseConfig
    );

    log.info("Cross-validation results:");
    log.info("Mean score: {} ± {}", cvResult.getMeanScore(), cvResult.getStdScore());
    log.info("Fold scores: {}", cvResult.getFoldScores());
  }

  /**
   * Example 4: Live prediction service
   */
  public void livePredictionExample(String symbol) {
    log.info("=== Example 4: Live Predictions ===");

    // Load trained model
    predictor.setModel(preprocessor.loadModel());

    // Make multi-horizon predictions
    List<GEXData> history = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
    );
    List<Integer> horizons = List.of(15, 30, 60);
    Map<Integer, PricePrediction> predictions = predictor.predictMultiHorizon(history.getLast(), history, horizons);

    log.info("Multi-horizon predictions:");
    predictions.forEach((horizon, pred) ->
        log.info("{}min: Price={}, Direction={}, Confidence={}",
            horizon,
            pred.getPredictedPrice(),
            pred.getDirection(),
            pred.getConfidence()
        ));

    // Probabilistic prediction with uncertainty
    ProbabilisticPrediction probPred = predictor.predictWithUncertainty(
        history.getLast(),
        history,
        60,
        100 // Monte Carlo samples
    );

    log.info("Probabilistic prediction:");
    log.info("  Mean: {}", probPred.getMeanPrediction());
    log.info("  Std Dev: {}", probPred.getStdDeviation());
    log.info("  95% CI: [{}, {}]",
        probPred.getConfidence95Lower(),
        probPred.getConfidence95Upper()
    );
  }

  /**
   * Example 5: Model comparison
   */
  public void modelComparisonExample(String symbol, LocalDateTime start, LocalDateTime end) {
    log.info("=== Example 5: Model Comparison ===");

    String[] modelTypes = {"feedforward", "lstm", "deep", "attention"};
    Map<String, Double> modelScores = new HashMap<>();

    TrainingConfig baseConfig = TrainingConfig.builder()
        .symbol(symbol)
        .startDate(start)
        .endDate(end)
        .predictionHorizon(60)
        .numEpochs(30)
        .batchSize(32)
        .learningRate(0.001)
        .seed(12345)
        .trainRatio(0.7)
        .validationRatio(0.15)
        .build();

    for (String modelType : modelTypes) {
      log.info("Training {} model...", modelType);
      TrainingConfig config = baseConfig.toBuilder().modelType(modelType).build();
      TrainingResult result = trainer.trainModel(config);
      modelScores.put(modelType, result.getTestRMSE());
      log.info("{} RMSE: {}", modelType, result.getTestRMSE());
    }

    // Find best model
    String bestModel = modelScores.entrySet().stream()
        .min(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("unknown");

    log.info("Best model: {} with RMSE: {}", bestModel, modelScores.get(bestModel));
  }

  private void visualizeTrainingProgress(TrainingResult result) {
    log.info("Training Progress:");
    log.info("Epoch | Train Loss | Valid Loss");
    log.info("------|------------|------------");
    for (int i = 0; i < result.getTrainLosses().size(); i++) {
      String info = String.format(
          "%5d | %10.6f | %10.6f",
          i + 1,
          result.getTrainLosses().get(i),
          result.getValidationLosses().get(i)
      );
      log.info(info);
    }
  }

  private List<GEXFeatures> extractAllFeatures(List<GEXData> snapshots) {
    GEXFeatureExtractor extractor = new GEXFeatureExtractor();
    List<GEXFeatures> features = new ArrayList<>();
    for (int i = 0; i < snapshots.size(); i++) {
      List<GEXData> history = snapshots.subList(Math.max(0, i - 60), i);
      features.add(extractor.extractFeatures(snapshots.get(i), history));
    }
    return features;
  }
}
