package net.dp.rpg.engine.interior.detail;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public record DetailStatistics(
    int runtimeId,
    Map<Integer, double[]> chanceByGround,
    int minSpacing) {

  public static final int DISTANCE_BUCKETS = 4;

  public DetailStatistics {
    Map<Integer, double[]> copy = new LinkedHashMap<>();

    chanceByGround.forEach((groundClass, chances) -> {
      if (chances.length != DISTANCE_BUCKETS) {
        throw new IllegalArgumentException("Expected %d distance buckets, got %d"
            .formatted(DISTANCE_BUCKETS, chances.length));
      }

      copy.put(groundClass, chances.clone());
    });

    chanceByGround = Collections.unmodifiableMap(copy);
  }

  public static int bucketOf(int wallDistance) {
    return Math.min(Math.max(wallDistance, 1), DISTANCE_BUCKETS) - 1;
  }

  public double chanceAt(int groundClass, int wallDistance) {
    double[] chances = chanceByGround.get(groundClass);

    return chances == null ? 0.0 : chances[bucketOf(wallDistance)];
  }
}
