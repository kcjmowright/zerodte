package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.BackTestResult;
import com.kcjmowright.zerodte.model.BackTestTrade;
import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.PricePrediction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GEXBackTester {

  private final GEXPricePredictor predictor;

  public BackTestResult runBacktest(List<GEXData> historicalData,
                                    int predictionHorizonMinutes,
                                    int minHistorySize) {
    List<BackTestTrade> trades = new ArrayList<>();
    Map<String, List<BigDecimal>> regimeErrors = new HashMap<>();
    regimeErrors.put("POSITIVE_GEX", new ArrayList<>());
    regimeErrors.put("NEGATIVE_GEX", new ArrayList<>());

    // Walk forward through historical data
    for (int i = minHistorySize; i < historicalData.size() - predictionHorizonMinutes; i++) {
      GEXData currentSnapshot = historicalData.get(i);
      List<GEXData> history = historicalData.subList(
          Math.max(0, i - 60), i
      );

      // Make prediction
      PricePrediction prediction = predictor.predict(
          currentSnapshot,
          history,
          predictionHorizonMinutes
      );

      // Get actual future price
      GEXData futureSnapshot = historicalData.get(i + predictionHorizonMinutes);
      BigDecimal actualPrice = futureSnapshot.getTotalGEX().getSpotPrice();

      // Record trade
      BackTestTrade trade = BackTestTrade.builder()
          .entryTime(currentSnapshot.getCreated())
          .entryPrice(currentSnapshot.getTotalGEX().getSpotPrice())
          .exitTime(futureSnapshot.getCreated())
          .actualPrice(actualPrice)
          .predictedPrice(prediction.getPredictedPrice())
          .direction(prediction.getDirection())
          .regime(prediction.getRegime())
          .confidence(prediction.getConfidence())
          .build();

      trades.add(trade);

      // Track regime-specific errors
      BigDecimal error = actualPrice.subtract(prediction.getPredictedPrice()).abs();
      regimeErrors.get(prediction.getRegime()).add(error);
    }

    // Calculate metrics
    return calculateMetrics(trades, historicalData, regimeErrors);
  }

  private BackTestResult calculateMetrics(List<BackTestTrade> trades,
                                          List<GEXData> data,
                                          Map<String, List<BigDecimal>> regimeErrors) {
    // Direction accuracy
    long correctDirections = trades.stream()
        .filter(this::isDirectionCorrect)
        .count();
    BigDecimal accuracy = BigDecimal.valueOf(correctDirections)
        .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);

    // Mean Absolute Error
    BigDecimal mae = trades.stream()
        .map(t -> t.getActualPrice().subtract(t.getPredictedPrice()).abs())
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);

    // RMSE
    BigDecimal sumSquaredErrors = trades.stream()
        .map(t -> {
          BigDecimal error = t.getActualPrice().subtract(t.getPredictedPrice());
          return error.multiply(error);
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal mse = sumSquaredErrors.divide(
        BigDecimal.valueOf(trades.size()),
        4,
        RoundingMode.HALF_UP
    );
    BigDecimal rmse = BigDecimal.valueOf(Math.sqrt(mse.doubleValue()));

    // Profit Factor (if trading based on predictions)
    BigDecimal profitFactor = calculateProfitFactor(trades);

    // Regime-specific performance
    Map<String, BigDecimal> regimePerformance = new HashMap<>();
    for (Map.Entry<String, List<BigDecimal>> entry : regimeErrors.entrySet()) {
      BigDecimal avgError = entry.getValue().stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(entry.getValue().size()), 4, RoundingMode.HALF_UP);
      regimePerformance.put(entry.getKey(), avgError);
    }

    return BackTestResult.builder()
        .startTime(data.getFirst().getCreated())
        .endTime(data.getLast().getCreated())
        .totalPredictions(trades.size())
        .accuracy(accuracy)
        .meanAbsoluteError(mae)
        .rmse(rmse)
        .profitFactor(profitFactor)
        .regimePerformance(regimePerformance)
        .build();
  }

  private boolean isDirectionCorrect(BackTestTrade trade) {
    BigDecimal actualMove = trade.getActualPrice().subtract(trade.getEntryPrice());
    BigDecimal predictedMove = trade.getPredictedPrice().subtract(trade.getEntryPrice());

    // Check if signs match
    return actualMove.signum() == predictedMove.signum();
  }

  private BigDecimal calculateProfitFactor(List<BackTestTrade> trades) {
    BigDecimal grossProfit = BigDecimal.ZERO;
    BigDecimal grossLoss = BigDecimal.ZERO;

    for (BackTestTrade trade : trades) {
      BigDecimal pnl = calculateTradePnL(trade);
      if (pnl.compareTo(BigDecimal.ZERO) > 0) {
        grossProfit = grossProfit.add(pnl);
      } else {
        grossLoss = grossLoss.add(pnl.abs());
      }
    }

    if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.valueOf(999);
    }
    return grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateTradePnL(BackTestTrade trade) {
    // Simulate trading based on prediction
    BigDecimal actualMove = trade.getActualPrice().subtract(trade.getEntryPrice());
    BigDecimal predictedMove = trade.getPredictedPrice().subtract(trade.getEntryPrice());

    // If we predicted correctly, profit is proportional to move
    if (actualMove.signum() == predictedMove.signum()) {
      return actualMove.abs();
    }
    return actualMove.abs().negate();
  }

  // Walk-forward optimization
  public Map<String, Object> walkForwardOptimization(
      List<GEXData> data,
      int trainingWindow,
      int testingWindow) {

    Map<String, Object> results = new HashMap<>();
    List<BackTestResult> foldResults = new ArrayList<>();

    for (int i = 0; i < data.size() - trainingWindow - testingWindow;
         i += testingWindow) {

      List<GEXData> trainData = data.subList(i, i + trainingWindow);
      List<GEXData> testData = data.subList(
          i + trainingWindow,
          i + trainingWindow + testingWindow
      );

      // Train/optimize on training data (placeholder)
      // In practice, you'd optimize hyperparameters here

      // Test on out-of-sample data
      BackTestResult foldResult = runBacktest(testData, 60, 10);
      foldResults.add(foldResult);
    }

    // Aggregate results
    BigDecimal avgAccuracy = foldResults.stream()
        .map(BackTestResult::getAccuracy)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(foldResults.size()), 4, RoundingMode.HALF_UP);

    results.put("folds", foldResults.size());
    results.put("averageAccuracy", avgAccuracy);
    results.put("foldResults", foldResults);
    return results;
  }
}
