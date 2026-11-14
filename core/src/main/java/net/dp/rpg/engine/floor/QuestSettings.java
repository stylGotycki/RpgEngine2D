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
    return new QuestSettings(0.30, List.of(1.0, 4.0, 3.0, 1.0));
  }

  public QuestSettings withRoomRatio(double ratio) {
    return new QuestSettings(ratio, bundleWeights);
  }

  // Quests of at most `size` rooms: a bundle of one room is one simple goal.
  public QuestSettings withMaxBundle(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("A bundle needs at least one room, got " + size);
    }

    return size >= bundleWeights.size() ? this
        : new QuestSettings(roomRatio, bundleWeights.subList(0, size));
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
