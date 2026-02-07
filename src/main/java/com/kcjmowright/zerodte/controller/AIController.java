package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.GEXData;
import com.kcjmowright.zerodte.model.PricePrediction;
import com.kcjmowright.zerodte.model.TrainingConfig;
import com.kcjmowright.zerodte.model.TrainingResult;
import com.kcjmowright.zerodte.service.GEXModelTrainer;
import com.kcjmowright.zerodte.service.GEXPredictor;
import com.kcjmowright.zerodte.service.GEXService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
@RequiredArgsConstructor
class AIController {

  private final GEXPredictor predictor;
  private final GEXModelTrainer trainer;
  private final GEXService gexService;

  @PostMapping("/train")
  public Mono<TrainingResult> trainModel(@RequestBody TrainingConfig config) {
    log.info("Received training request: {}", config);
    TrainingResult result = trainer.trainModel(config);
    return Mono.just(result);
  }

  @GetMapping("/predict/{symbol}")
  public Mono<PricePrediction> predict(@PathVariable String symbol, @RequestParam int minutesAhead) {
    PricePrediction prediction = predictor.predictLive(symbol, minutesAhead);
    return Mono.just(prediction);
  }

  @GetMapping("/predict/multi-horizon/{symbol}")
  public Mono<Map<Integer, PricePrediction>> predictMultiHorizon(@PathVariable String symbol) {
    List<GEXData> history = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        LocalDateTime.now().minusDays(4),
        LocalDateTime.now()
    );
    Map<Integer, PricePrediction> predictions = predictor.predictMultiHorizon(
        history.getLast(),
        history,
        List.of(15, 30, 60)
    );
    return Mono.just(predictions);
  }

  @GetMapping("/feature-importance/{symbol}")
  public Mono<Map<String, Double>> getFeatureImportance(@PathVariable String symbol) {
    List<GEXData> testData = gexService.getGEXDataBySymbolBetweenStartAndEnd(
        symbol,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
    );
    Map<String, Double> importance = predictor.calculateFeatureImportance(testData, 60);
    return Mono.just(importance);
  }
}
