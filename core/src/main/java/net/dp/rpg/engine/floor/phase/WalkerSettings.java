package net.dp.rpg.engine.floor.phase;

public record WalkerSettings(
    double straightWeight,
    double turnWeight,
    double reverseWeight,
    double freeCellWeight,
    double sameRoomWeight,
    double otherRoomWeight,
    double beyondTargetWeight,
    int maxBarrenSteps,
    int hardStepCap) {

  public WalkerSettings {
    requirePositive(straightWeight, "straightWeight");
    requirePositive(turnWeight, "turnWeight");
    requirePositive(reverseWeight, "reverseWeight");
    requirePositive(freeCellWeight, "freeCellWeight");
    requirePositive(sameRoomWeight, "sameRoomWeight");
    requirePositive(otherRoomWeight, "otherRoomWeight");

    if (!Double.isFinite(beyondTargetWeight) || beyondTargetWeight < 0.0) {
      throw new IllegalArgumentException("beyondTargetWeight must be zero or more, got "
          + beyondTargetWeight);
    }

    if (maxBarrenSteps < 1 || hardStepCap < 1) {
      throw new IllegalArgumentException("Step limits must be positive");
    }
  }

  public static WalkerSettings defaults() {
    return new WalkerSettings(0.55, 0.20, 0.05, 4.0, 1.0, 1.0, 0.0, 60, 20_000);
  }

  public WalkerSettings withBeyondTargetWeight(double weight) {
    return new WalkerSettings(straightWeight, turnWeight, reverseWeight, freeCellWeight,
        sameRoomWeight, otherRoomWeight, weight, maxBarrenSteps, hardStepCap);
  }

  // Towards a walker that hardly ever steps back into older rooms, so the trunk stays one line.
  public WalkerSettings towardsLinear(double linearity) {
    if (linearity <= 0) {
      return this;
    }

    double l = Math.min(1, linearity);
    double reverse = reverseWeight + (0.001 - reverseWeight) * l;
    double other = otherRoomWeight + (0.01 - otherRoomWeight) * l;

    return new WalkerSettings(straightWeight, turnWeight, reverse, freeCellWeight * (1 + 5 * l),
        sameRoomWeight, other, beyondTargetWeight, maxBarrenSteps, hardStepCap);
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException("%s must be greater than zero, got %s".formatted(name, value));
    }
  }
}
