package com.kcjmowright.zerodte.service;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DropoutLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GEXModelBuilder {

  /**
   * Simple Feed-Forward Neural Network
   * Best for: Basic price prediction without temporal dependencies
   */
  public MultiLayerNetwork buildFeedForwardNetwork(int numInputs, int seed, double learningRate, double l2) {
    log.info("Building Feed-Forward Network with {} inputs", numInputs);

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(learningRate))
        .l2(l2) // L2 regularization
        .list()
        // Input layer
        .layer(new DenseLayer.Builder()
            .nIn(numInputs)
            .nOut(128)
            .activation(Activation.RELU)
            .dropOut(0.2)
            .build())
        // Hidden layer 1
        .layer(new DenseLayer.Builder()
            .nIn(128)
            .nOut(64)
            .activation(Activation.RELU)
            .dropOut(0.2)
            .build())
        // Hidden layer 2
        .layer(new DenseLayer.Builder()
            .nIn(64)
            .nOut(32)
            .activation(Activation.RELU)
            .dropOut(0.1)
            .build())
        // Output layer (regression)
        .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(32)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    log.info("Model parameters: {}", model.numParams());
    return model;
  }

  /**
   * LSTM Network
   * Best for: Capturing temporal patterns in GEX data
   */
  public MultiLayerNetwork buildLSTMNetwork(int numInputs, int sequenceLength, int seed, double learningRate, double l2) {
    log.info("Building LSTM Network with {} inputs, sequence length {}",
        numInputs, sequenceLength);

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(learningRate))
        .l2(l2)
        .list()
        // LSTM layer 1
        .layer(new LSTM.Builder()
            .nIn(numInputs)
            .nOut(64)
            .activation(Activation.TANH)
            .build())
        // LSTM layer 2
        .layer(new LSTM.Builder()
            .nIn(64)
            .nOut(32)
            .activation(Activation.TANH)
            .build())
        // Convert RNN output to feed-forward
        .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(32)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    log.info("LSTM Model parameters: {}", model.numParams());
    return model;
  }

  /**
   * Deep Feed-Forward with Batch Normalization
   * Best for: Complex non-linear GEX relationships
   */
  public MultiLayerNetwork buildDeepNetwork(int numInputs, int seed, double learningRate, double l2) {
    log.info("Building Deep Network with {} inputs", numInputs);

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(learningRate))
        .l2(l2)
        .list()
        // Layer 1
        .layer(new DenseLayer.Builder()
            .nIn(numInputs)
            .nOut(256)
            .activation(Activation.RELU)
            .build())
        .layer(new BatchNormalization.Builder().build())
        .layer(new DropoutLayer.Builder(0.3).build())

        // Layer 2
        .layer(new DenseLayer.Builder()
            .nIn(256)
            .nOut(128)
            .activation(Activation.RELU)
            .build())
        .layer(new BatchNormalization.Builder().build())
        .layer(new DropoutLayer.Builder(0.3).build())

        // Layer 3
        .layer(new DenseLayer.Builder()
            .nIn(128)
            .nOut(64)
            .activation(Activation.RELU)
            .build())
        .layer(new BatchNormalization.Builder().build())
        .layer(new DropoutLayer.Builder(0.2).build())

        // Layer 4
        .layer(new DenseLayer.Builder()
            .nIn(64)
            .nOut(32)
            .activation(Activation.RELU)
            .build())

        // Output
        .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(32)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    log.info("Deep Model parameters: {}", model.numParams());
    return model;
  }

  /**
   * Bidirectional LSTM
   * Best for: Understanding both past and future context
   */
  public MultiLayerNetwork buildBidirectionalLSTM(int numInputs, int seed) {
    log.info("Building Bidirectional LSTM with {} inputs", numInputs);

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(0.001))
        .l2(0.0001)
        .list()
        // Bidirectional LSTM
        .layer(new Bidirectional(new LSTM.Builder()
            .nIn(numInputs)
            .nOut(32)
            .activation(Activation.TANH)
            .build()))

        // Regular LSTM
        .layer(new LSTM.Builder()
            .nIn(64) // 32*2 from bidirectional
            .nOut(32)
            .activation(Activation.TANH)
            .build())

        // Output
        .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(32)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    log.info("Bidirectional LSTM parameters: {}", model.numParams());
    return model;
  }

  /**
   * Attention-based Network
   * Best for: Focusing on most important GEX features
   */
  public MultiLayerNetwork buildAttentionNetwork(int numInputs, int seed) {
    log.info("Building Attention Network with {} inputs", numInputs);

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(0.001))
        .l2(0.0001)
        .list()
        // Feature extraction
        .layer(new DenseLayer.Builder()
            .nIn(numInputs)
            .nOut(128)
            .activation(Activation.RELU)
            .build())

        // Self-attention mechanism (simplified)
        .layer(new DenseLayer.Builder()
            .nIn(128)
            .nOut(128)
            .activation(Activation.SOFTMAX)
            .build())

        // Value transformation
        .layer(new DenseLayer.Builder()
            .nIn(128)
            .nOut(64)
            .activation(Activation.RELU)
            .dropOut(0.2)
            .build())

        .layer(new DenseLayer.Builder()
            .nIn(64)
            .nOut(32)
            .activation(Activation.RELU)
            .build())

        // Output
        .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(32)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    log.info("Attention Model parameters: {}", model.numParams());
    return model;
  }

  /**
   * Ensemble-ready model with multiple outputs
   * Predicts: price change, direction, confidence
   */
  public MultiLayerNetwork buildMultiOutputNetwork(int numInputs, int seed) {
    log.info("Building Multi-Output Network with {} inputs", numInputs);

    // Note: DL4J multi-output requires ComputationGraph instead
    // This is a simplified version with single output

    MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
        .seed(seed)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(0.001))
        .l2(0.0001)
        .list()
        .layer(new DenseLayer.Builder()
            .nIn(numInputs)
            .nOut(128)
            .activation(Activation.RELU)
            .dropOut(0.2)
            .build())
        .layer(new DenseLayer.Builder()
            .nIn(128)
            .nOut(64)
            .activation(Activation.RELU)
            .dropOut(0.2)
            .build())
        .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
            .nIn(64)
            .nOut(1)
            .activation(Activation.IDENTITY)
            .build())
        .build();

    MultiLayerNetwork model = new MultiLayerNetwork(config);
    model.init();

    return model;
  }

  /**
   * Get recommended model based on data characteristics
   */
  public MultiLayerNetwork buildRecommendedModel(int numInputs, int numSamples, boolean hasTemporalData, int seed, double learningRate, double l2) {
    log.info("Building recommended model for {} samples, {} inputs",
        numSamples, numInputs);
    if (numSamples < 1000) {
      log.info("Small dataset - using simple feed-forward network");
      return buildFeedForwardNetwork(numInputs, seed, learningRate, l2);
    }
    if (hasTemporalData && numSamples > 5000) {
      log.info("Large temporal dataset - using LSTM");
      return buildLSTMNetwork(numInputs, 10, seed, learningRate, l2);
    }
    if (numSamples > 10000) {
      log.info("Large dataset - using deep network");
      return buildDeepNetwork(numInputs, seed, learningRate, l2);
    }
    log.info("Medium dataset - using feed-forward network");
    return buildFeedForwardNetwork(numInputs, seed, learningRate, l2);
  }
}
