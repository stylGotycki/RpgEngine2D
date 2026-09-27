package net.dp.rpg.engine.floor;

public record TrunkPlan(int roomBudget, int cellBudget, ShapePool shapes, boolean exclusiveGroups) {

  public static final double DEFAULT_CELLS_PER_ROOM = 1.8;

  public TrunkPlan {
    if (roomBudget < 1) {
      throw new IllegalArgumentException("Room budget must be positive, got " + roomBudget);
    }

    if (cellBudget < roomBudget) {
      throw new IllegalArgumentException("Cell budget %d cannot be below room budget %d"
          .formatted(cellBudget, roomBudget));
    }
  }

  public static TrunkPlan of(int roomBudget) {
    return of(roomBudget, DEFAULT_CELLS_PER_ROOM, ShapePools.STANDARD);
  }

  public static TrunkPlan of(int roomBudget, double cellsPerRoom, ShapePool shapes) {
    return new TrunkPlan(roomBudget, (int) Math.round(roomBudget * cellsPerRoom), shapes, true);
  }

  public TrunkPlan withExclusiveGroups(boolean enabled) {
    return new TrunkPlan(roomBudget, cellBudget, shapes, enabled);
  }
}
