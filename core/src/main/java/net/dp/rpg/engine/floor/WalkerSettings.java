package net.dp.rpg.engine.floor;

public record WalkerSettings(
    double straightWeight,
    double turnWeight,
    double reverseWeight,
    double freeCellWeight,
    double sameRoomWeight,
    double otherRoomWeight,
    double loopChance,
    int maxBarrenSteps,
    int hardStepCap) {

  public WalkerSettings {
    requirePositive(straightWeight, "straightWeight");
    requirePositive(turnWeight, "turnWeight");
    requirePositive(reverseWeight, "reverseWeight");
    requirePositive(freeCellWeight, "freeCellWeight");
    requirePositive(sameRoomWeight, "sameRoomWeight");
    requirePositive(otherRoomWeight, "otherRoomWeight");

    if (loopChance < 0.0 || loopChance > 1.0) {
      throw new IllegalArgumentException("loopChance must be within 0..1, got " + loopChance);
    }

    if (maxBarrenSteps < 1 || hardStepCap < 1) {
      throw new IllegalArgumentException("Step limits must be positive");
    }
  }

  public static WalkerSettings defaults() {
    return new WalkerSettings(0.55, 0.20, 0.05, 4.0, 1.0, 1.0, 0.30, 60, 20_000);
  }

  public WalkerSettings withLoopChance(double chance) {
    return new WalkerSettings(straightWeight, turnWeight, reverseWeight, freeCellWeight, sameRoomWeight,
        otherRoomWeight, chance, maxBarrenSteps, hardStepCap);
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0.0) {
      throw new IllegalArgumentException("%s must be greater than zero, got %s".formatted(name, value));
    }
  }
}
