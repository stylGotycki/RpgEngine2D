package net.dp.rpg.engine.floor;

public record LoopSettings(double loopsPerRoom, int minCycleRooms, double maxCriticalLoss) {

  public LoopSettings {
    if (!Double.isFinite(loopsPerRoom) || loopsPerRoom < 0.0) {
      throw new IllegalArgumentException("loopsPerRoom must be zero or more, got " + loopsPerRoom);
    }

    if (minCycleRooms < 3) {
      throw new IllegalArgumentException("A cycle needs at least 3 rooms, got " + minCycleRooms);
    }

    if (maxCriticalLoss < 0.0 || maxCriticalLoss >= 1.0) {
      throw new IllegalArgumentException("maxCriticalLoss must be within 0..1, got " + maxCriticalLoss);
    }
  }

  public static LoopSettings defaults() {
    return new LoopSettings(0.15, 4, 0.1);
  }

  public static LoopSettings none() {
    return new LoopSettings(0.0, 3, 0.0);
  }

  public int loopsFor(int roomCount) {
    return (int) Math.round(roomCount * loopsPerRoom);
  }
}
