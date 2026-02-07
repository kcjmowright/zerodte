package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.GEXFeatures;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.TotalGEX;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GEXPricePredictor {

  private final GEXFeatureExtractor featureExtractor;
  private final GEXDataPreprocessor dataPreprocessor;

  public static final String POSITIVE_GEX = "POSITIVE_GEX";
  public static final String NEGATIVE_GEX = "NEGATIVE_GEX";
  public static final String UP = "UP";
  public static final String DOWN = "DOWN";
  public static final String NEUTRAL = "NEUTRAL";

  public PricePrediction predict(GEXData currentSnapshot,
                                 List<GEXData> historicalSnapshots,
                                 int minutesAhead) {

    GEXFeatures features = featureExtractor.extractFeatures(
        currentSnapshot,
        historicalSnapshots
    );

    String regime = determineRegime(currentSnapshot.getTotalGEX(), features);
    BigDecimal predictedMove = calculatePredictedMove(features, regime, minutesAhead);
    BigDecimal predictedPrice = currentSnapshot.getTotalGEX().getSpotPrice().add(predictedMove);

    return PricePrediction.builder()
        .predictionTime(currentSnapshot.getTotalGEX().getTimestamp())
        .targetTime(currentSnapshot.getTotalGEX().getTimestamp().plusMinutes(minutesAhead))
        .predictedPrice(predictedPrice)
        .confidence(calculateConfidence(features))
        .direction(determineDirection(predictedMove))
        .expectedMove(predictedMove.abs())
        .regime(regime)
        .build();
  }

  private String determineRegime(TotalGEX snapshot, GEXFeatures features) {
    // Determine if we're in positive or negative GEX regime
    return (snapshot.getSpotPrice().compareTo(snapshot.getFlipPoint()) > 0) ? POSITIVE_GEX : NEGATIVE_GEX;
  }

  private BigDecimal calculatePredictedMove(GEXFeatures features,
                                            String regime,
                                            int minutesAhead) {

    BigDecimal baseMove = BigDecimal.ZERO;

    // 1. Gravity toward walls (dominant force)
    baseMove = baseMove.add(calculateWallGravity(features));

    // 2. Momentum component
    baseMove = baseMove.add(calculateMomentumEffect(features, regime));

    // 3. Time decay effect (stronger as expiry approaches)
    baseMove = baseMove.multiply(calculateTimeDecayMultiplier(features));

    // 4. Regime-based volatility adjustment
    if (NEGATIVE_GEX.equalsIgnoreCase(regime)) {
      // Amplify moves in negative GEX
      baseMove = baseMove.multiply(BigDecimal.valueOf(1.3));
    } else {
      // Dampen moves in positive GEX
      baseMove = baseMove.multiply(BigDecimal.valueOf(0.7));
    }

    // 5. Scale to time horizon
    BigDecimal timeScale = BigDecimal.valueOf(minutesAhead)
        .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    baseMove = baseMove.multiply(timeScale);

    return baseMove;
  }

  private BigDecimal calculateWallGravity(GEXFeatures features) {
    // Price tends to move toward the nearest wall
    BigDecimal callDist = features.getDistanceToCallWall();
    BigDecimal putDist = features.getDistanceToPutWall();

    // Stronger gravity from closer wall
    BigDecimal callGravity = callDist.multiply(BigDecimal.valueOf(0.15));
    BigDecimal putGravity = putDist.multiply(BigDecimal.valueOf(0.15));

    // Net effect
    BigDecimal netGravity = callGravity.add(putGravity);

    // Weight by concentration (more concentrated = stronger effect)
    return netGravity.multiply(features.getConcentrationIndex());
  }

  private BigDecimal calculateMomentumEffect(GEXFeatures features, String regime) {
    BigDecimal velocity = features.getPriceVelocity();
    return NEGATIVE_GEX.equalsIgnoreCase(regime) ?
        // Momentum persists in negative GEX
        velocity.multiply(BigDecimal.valueOf(0.8)) :
        // Mean reversion in positive GEX
        velocity.multiply(BigDecimal.valueOf(-0.3));
  }

  private BigDecimal calculateTimeDecayMultiplier(GEXFeatures features) {
    // As expiry approaches, gamma effects intensify
    int minutesLeft = features.getMinutesToExpiry();

    if (minutesLeft < 60) {
      // Last hour: strong pinning effect
      return BigDecimal.valueOf(1.5);
    }
    if (minutesLeft < 120) {
      // 1-2 hours: moderate effect
      return BigDecimal.valueOf(1.2);
    }
    // Earlier in day: normal behavior
    return BigDecimal.valueOf(1.0);

  }

  private BigDecimal calculateConfidence(GEXFeatures features) {
    // Higher confidence when:
    // - High GEX concentration
    // - Clear position relative to walls
    // - Consistent regime

    BigDecimal concentration = features.getConcentrationIndex();
    BigDecimal position = features.getRelativePosition();

    // Distance from 0.5 indicates clear positioning
    BigDecimal positionClarity = position.subtract(BigDecimal.valueOf(0.5))
        .abs()
        .multiply(BigDecimal.valueOf(2));

    BigDecimal confidence = concentration.multiply(BigDecimal.valueOf(0.6))
        .add(positionClarity.multiply(BigDecimal.valueOf(0.4)));

    // Cap at 1.0
    return confidence.min(BigDecimal.ONE);
  }

  private String determineDirection(BigDecimal predictedMove) {
    if (predictedMove.compareTo(BigDecimal.valueOf(0.1)) > 0) {
      return UP;
    }
    if (predictedMove.compareTo(BigDecimal.valueOf(-0.1)) < 0) {
      return DOWN;
    }
    return NEUTRAL;
  }

//  // Alternative ML-based prediction (placeholder for trained model)
//  public PricePrediction predictWithModel(GEXFeatures features,
//                                          TrainedModel model) {
//    // This would integrate with a trained ML model (e.g., XGBoost, Random Forest)
//    // trained on historical GEX -> price movement relationships
//
//    double[] featureVector = convertToFeatureVector(features);
//    double prediction = model.predict(featureVector);
//
//    // Convert to PricePrediction object
//    return null; // Implementation depends on your ML framework
//  }

//  private double[] convertToFeatureVector(GEXFeatures features) {
//    return new double[]{
//        features.getDistanceToCallWall().doubleValue(),
//        features.getDistanceToPutWall().doubleValue(),
//        features.getDistanceToFlipPoint().doubleValue(),
//        features.getCallPutGEXRatio().doubleValue(),
//        features.getNetGEX().doubleValue(),
//        features.getGexSkew().doubleValue(),
//        features.getConcentrationIndex().doubleValue(),
//        features.getRelativePosition().doubleValue(),
//        features.getMinutesToExpiry().doubleValue(),
//        features.getPriceVelocity().doubleValue(),
//    };
//  }
}
