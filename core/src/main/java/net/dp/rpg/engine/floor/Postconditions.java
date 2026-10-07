package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.tile.room.RoomCell;

public record Postconditions(
    int minCriticalLength,
    int minDeadEnds,
    int maxBoundingCells,
    boolean requireAllAbilities) {

  public static Postconditions defaults() {
    return new Postconditions(5, 3, Integer.MAX_VALUE, true);
  }

  public static Postconditions none() {
    return new Postconditions(0, 0, Integer.MAX_VALUE, false);
  }

  public Postconditions withMinCriticalLength(int length) {
    return new Postconditions(length, minDeadEnds, maxBoundingCells, requireAllAbilities);
  }

  public Postconditions withMinDeadEnds(int count) {
    return new Postconditions(minCriticalLength, count, maxBoundingCells, requireAllAbilities);
  }

  /** Reasons this floor is not acceptable. An empty list means it is. */
  public List<String> violations(FloorLayout floor) {
    List<String> violations = new ArrayList<>();

    if (floor.criticalLength() < minCriticalLength) {
      violations.add("critical path %d < %d".formatted(floor.criticalLength(), minCriticalLength));
    }

    int deadEnds = floor.graph().deadEnds().size();

    if (deadEnds < minDeadEnds) {
      violations.add("dead ends %d < %d".formatted(deadEnds, minDeadEnds));
    }

    int bounding = boundingCells(floor);

    if (bounding > maxBoundingCells) {
      violations.add("bounding box %d > %d".formatted(bounding, maxBoundingCells));
    }

    if (floor.boss() == null) {
      violations.add("no boss room");
    }

    if (requireAllAbilities && !floor.unspentAbilities().isEmpty()) {
      violations.add("unspent abilities " + floor.unspentAbilities());
    }

    return violations;
  }

  private int boundingCells(FloorLayout floor) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (RoomNode room : floor.graph().rooms()) {
      for (RoomCell cell : room.cells()) {
        minX = Math.min(minX, cell.x());
        minY = Math.min(minY, cell.y());
        maxX = Math.max(maxX, cell.x());
        maxY = Math.max(maxY, cell.y());
      }
    }

    return minX > maxX ? 0 : (maxX - minX + 1) * (maxY - minY + 1);
  }
}