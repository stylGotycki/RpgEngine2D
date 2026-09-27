package net.dp.rpg.engine.floor.phase;

import java.util.Random;

import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;

public final class Walker {

  private final GridBounds bounds;

  private final WalkerSettings settings;

  private RoomCell cell;

  private Direction heading;

  public Walker(RoomCell start, GridBounds bounds, WalkerSettings settings) {
    this.cell = start;
    this.bounds = bounds;
    this.settings = settings;
  }

  public RoomCell cell() {
    return cell;
  }

  public Direction heading() {
    return heading;
  }

  public Direction chooseDirection(FloorGraph graph, Random random) {
    return chooseDirection(graph, random, null);
  }

  public Direction chooseDirection(FloorGraph graph, Random random, Direction excluded) {
    Direction[] directions = Direction.values();
    double[] weights = new double[directions.length];
    double total = 0.0;

    for (int index = 0; index < directions.length; index++) {
      Direction direction = directions[index];

      if (direction == excluded) {
        continue;
      }

      RoomCell target = cell.neighbour(direction);

      if (!bounds.contains(target)) {
        continue;
      }

      double weight = momentumOf(direction) * noveltyOf(graph, target);

      weights[index] = weight;
      total += weight;
    }

    if (total <= 0.0) {
      return null;
    }

    double roll = random.nextDouble() * total;

    for (int index = 0; index < directions.length; index++) {
      roll -= weights[index];

      if (roll <= 0.0 && weights[index] > 0.0) {
        return directions[index];
      }
    }

    return lastNonZero(directions, weights);
  }

  public void moveTo(RoomCell target, Direction direction) {
    this.cell = target;
    this.heading = direction;
  }

  private double momentumOf(Direction direction) {
    if (heading == null) {
      return 1.0;
    }

    if (direction == heading) {
      return settings.straightWeight();
    }

    return direction == heading.opposite() ? settings.reverseWeight() : settings.turnWeight();
  }

  private double noveltyOf(FloorGraph graph, RoomCell target) {
    RoomNode targetRoom = graph.roomAt(target);

    if (targetRoom == null) {
      return graph.isReserved(target) ? 0.0 : settings.freeCellWeight();
    }

    return targetRoom == graph.roomAt(cell) ? settings.sameRoomWeight() : settings.otherRoomWeight();
  }

  private static Direction lastNonZero(Direction[] directions, double[] weights) {
    for (int index = directions.length - 1; index >= 0; index--) {
      if (weights[index] > 0.0) {
        return directions[index];
      }
    }

    return null;
  }
}
