package net.dp.rpg.engine.interior.model;

import java.util.Arrays;
import net.dp.rpg.engine.wfc.WeightTable;

public final class ZoneStatistics {

  static final double SMOOTHING = 8.0;

  private final double[] counts;

  private ZoneStatistics(double[] counts) {
    this.counts = counts;
  }

  static ZoneStatistics zero(int tokenCount) {
    return new ZoneStatistics(new double[tokenCount]);
  }

  void add(int token, double amount) {
    counts[token] += amount;
  }

  double count(int token) {
    return counts[token];
  }

  double total() {
    double total = 0.0;

    for (double count : counts) {
      total += count;
    }

    return total;
  }

  WeightTable toWeightTable(ZoneStatistics zoneWide) {
    double[] weights = new double[counts.length];
    double zoneTotal = zoneWide.total();

    for (int token = 0; token < weights.length; token++) {
      if (zoneWide.counts[token] <= 0.0) {
        continue;
      }

      double prior = zoneTotal > 0.0 ? zoneWide.counts[token] / zoneTotal : 0.0;

      weights[token] = counts[token] + prior * SMOOTHING;
    }

    return WeightTable.of(weights);
  }

  @Override
  public String toString() {
    return Arrays.toString(counts);
  }
}
