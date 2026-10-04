package net.dp.rpg.engine.wfc;

import java.util.Arrays;

public final class WeightTable {

  private final double[] weights;

  private final double[] weightLogWeights;

  private final TokenSet support;

  private WeightTable(double[] weights) {
    this.weights = weights;
    this.weightLogWeights = new double[weights.length];

    int[] positive = new int[weights.length];
    int count = 0;

    for (int token = 0; token < weights.length; token++) {
      double weight = weights[token];

      if (!Double.isFinite(weight) || weight < 0.0) {
        throw new IllegalArgumentException("Weight of token %d must be finite and non-negative: %s"
            .formatted(token, weight));
      }

      if (weight > 0.0) {
        weightLogWeights[token] = weight * StrictMath.log(weight);
        positive[count++] = token;
      }
    }

    if (count == 0) {
      throw new IllegalArgumentException("Weight table must give at least one token a weight");
    }

    this.support = TokenSet.of(weights.length, Arrays.copyOf(positive, count));
  }

  public static WeightTable of(double... weights) {
    if (weights == null || weights.length == 0) {
      throw new IllegalArgumentException("Weight table must cover at least one token");
    }

    return new WeightTable(weights.clone());
  }

  public static WeightTable uniform(int tokenCount) {
    if (tokenCount <= 0) {
      throw new IllegalArgumentException("Token count must be greater than zero: " + tokenCount);
    }

    double[] weights = new double[tokenCount];

    Arrays.fill(weights, 1.0);

    return of(weights);
  }

  public int tokenCount() {
    return weights.length;
  }

  public double weight(int token) {
    return weights[token];
  }

  public TokenSet support() {
    return support;
  }

  double weightLogWeight(int token) {
    return weightLogWeights[token];
  }
}
