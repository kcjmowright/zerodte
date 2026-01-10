package com.kcjmowright.zerodte.controller;


import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.TotalGEX;
import com.kcjmowright.zerodte.model.TrainingConfig;
import com.kcjmowright.zerodte.model.TrainingResult;
import com.kcjmowright.zerodte.service.DL4JGEXPredictor;
import com.kcjmowright.zerodte.service.GEXModelTrainer;
import com.kcjmowright.zerodte.service.GammaExposureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dl4j")
@Slf4j
@RequiredArgsConstructor
class DL4JController {

  private final DL4JGEXPredictor predictor;
  private final GEXModelTrainer trainer;
  private final GammaExposureService gammaExposureService;

  @PostMapping("/train")
  public ResponseEntity<TrainingResult> trainModel(@RequestBody TrainingConfig config) throws Exception {
    log.info("Received training request: {}", config);
    TrainingResult result = trainer.trainModel(config);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/predict/{symbol}")
  public ResponseEntity<PricePrediction> predict(@PathVariable("symbol") String symbol, @RequestParam int minutesAhead) {
    PricePrediction prediction = predictor.predictLive(symbol, minutesAhead);
    return ResponseEntity.ok(prediction);
  }

  @PostMapping("/predict/multi-horizon/{symbol}")
  public ResponseEntity<Map<Integer, PricePrediction>> predictMultiHorizon(@PathVariable("symbol") String symbol) {
    TotalGEX current = gammaExposureService.getLatestBySymbol(symbol);
    List<TotalGEX> history = gammaExposureService.getMostRecentBySymbol(symbol, 1000);
    Map<Integer, PricePrediction> predictions = predictor.predictMultiHorizon(
        current,
        history,
        List.of(15, 30, 60)
    );
    return ResponseEntity.ok(predictions);
  }

  @GetMapping("/feature-importance/{symbol}")
  public ResponseEntity<Map<String, Double>> getFeatureImportance(@PathVariable("symbol") String symbol) {
    List<TotalGEX> testData = gammaExposureService.getMostRecentBySymbol(symbol, 1000);
    Map<String, Double> importance = predictor.calculateFeatureImportance(testData, 60);
    return ResponseEntity.ok(importance);
  }
}
