package net.dp.rpg.engine.floor;

import java.util.List;
import java.util.Random;

public record QuestSettings(double roomRatio, List<Double> bundleWeights) {

  public QuestSettings {
    if (roomRatio < 0.0 || roomRatio > 1.0) {
      throw new IllegalArgumentException("roomRatio must be within 0..1, got " + roomRatio);
    }

    if (bundleWeights.isEmpty()) {
      throw new IllegalArgumentException("At least one bundle size must be allowed");
    }

    bundleWeights = List.copyOf(bundleWeights);
  }

  public static QuestSettings defaults() {
    return new QuestSettings(0.30, List.of(8.0, 4.0, 2.0, 1.0));
  }

  public QuestSettings withRoomRatio(double ratio) {
    return new QuestSettings(ratio, bundleWeights);
  }

  public int maxBundleSize() {
    return bundleWeights.size();
  }

  public int drawSize(Random random) {
    double total = bundleWeights.stream().mapToDouble(Double::doubleValue).sum();
    double roll = random.nextDouble() * total;

    for (int index = 0; index < bundleWeights.size(); index++) {
      roll -= bundleWeights.get(index);

      if (roll <= 0.0) {
        return index + 1;
      }
    }

    return bundleWeights.size();
  }
}
