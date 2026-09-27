package net.dp.rpg.engine.floor;

public record FloorPlan(
    int roomCount,
    double trunkRatio,
    double cellsPerRoom,
    ShapePool defaultShapes,
    ShapePool bossShapes,
    boolean exclusiveGroups) {

  public static final double DEFAULT_TRUNK_RATIO = 0.70;

  public static final double DEFAULT_CELLS_PER_ROOM = 1.8;

  public FloorPlan {
    if (roomCount < 2) {
      throw new IllegalArgumentException("A floor needs at least two rooms, got " + roomCount);
    }

    if (trunkRatio <= 0.0 || trunkRatio > 1.0) {
      throw new IllegalArgumentException("trunkRatio must be within 0..1, got " + trunkRatio);
    }

    if (cellsPerRoom < 1.0) {
      throw new IllegalArgumentException("cellsPerRoom must be at least 1, got " + cellsPerRoom);
    }
  }

  public static FloorPlan of(int roomCount) {
    return new FloorPlan(roomCount, DEFAULT_TRUNK_RATIO, DEFAULT_CELLS_PER_ROOM,
        ShapePools.STANDARD, ShapePools.ARENA, true);
  }

  public FloorPlan withTrunkRatio(double ratio) {
    return new FloorPlan(roomCount, ratio, cellsPerRoom, defaultShapes, bossShapes, exclusiveGroups);
  }

  public FloorPlan withCellsPerRoom(double cells) {
    return new FloorPlan(roomCount, trunkRatio, cells, defaultShapes, bossShapes, exclusiveGroups);
  }

  public int trunkRooms() {
    return Math.max(1, Math.min(roomCount - 1, (int) Math.round(roomCount * trunkRatio)));
  }

  public int appendixRooms() {
    return roomCount - trunkRooms();
  }

  public int cellBudget() {
    return (int) Math.round(roomCount * cellsPerRoom);
  }

  public TrunkPlan trunkPlan() {
    return new TrunkPlan(trunkRooms(), cellBudget() - appendixRooms(), defaultShapes, exclusiveGroups);
  }
}
