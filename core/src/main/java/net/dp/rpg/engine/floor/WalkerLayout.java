package net.dp.rpg.engine.floor;

import java.util.Random;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class WalkerLayout {

  public WalkResult grow(int trunkBudget, GridBounds bounds, WalkerSettings settings, long seed) {
    if (trunkBudget < 1) {
      throw new IllegalArgumentException("Trunk budget must be positive, got " + trunkBudget);
    }

    Random layoutRandom = RandomSource.derive(seed, "layout");
    Random loopRandom = RandomSource.derive(seed, "loop");

    FloorGraph graph = new FloorGraph();
    RoomCell origin = bounds.center();

    graph.place(RoomShape.single(), origin);

    Walker walker = new Walker(origin, bounds, settings);

    int steps = 0;
    int barrenSteps = 0;
    StopReason reason = null;

    while (reason == null) {
      if (graph.size() >= trunkBudget) {
        reason = StopReason.BUDGET_REACHED;
        break;
      }

      if (barrenSteps >= settings.maxBarrenSteps()) {
        reason = StopReason.BARREN;
        break;
      }

      if (steps >= settings.hardStepCap()) {
        reason = StopReason.STEP_CAP;
        break;
      }

      Direction direction = walker.chooseDirection(graph, layoutRandom);

      if (direction == null) {
        reason = StopReason.BLOCKED;
        break;
      }

      steps++;

      boolean created = step(graph, walker, direction, loopRandom, settings);

      barrenSteps = created ? 0 : barrenSteps + 1;
    }

    int loops = closeLoops(graph, loopRandom, settings);

    return new WalkResult(graph, steps, loops, reason);
  }

  private int closeLoops(FloorGraph graph, Random loopRandom, WalkerSettings settings) {
    int opened = 0;

    for (RoomNode room : graph.rooms()) {
      for (RoomEdge edge : room.outerEdges()) {
        RoomNode other = graph.roomAt(edge.cell().neighbour(edge.direction()));

        boolean candidate = other != null
            && other.index() > room.index()
            && !room.isLinkedTo(other);

        if (candidate && loopRandom.nextDouble() < settings.loopChance()) {
          graph.connect(room, other, edge, DoorType.NORMAL);
          opened++;
        }
      }
    }

    return opened;
  }

  private boolean step(FloorGraph graph, Walker walker, Direction direction, Random loopRandom,
                       WalkerSettings settings) {
    RoomCell from = walker.cell();
    RoomCell target = from.neighbour(direction);
    RoomNode currentRoom = graph.roomAt(from);

    if (graph.isOccupied(target)) {
      crossIntoExisting(graph, currentRoom, graph.roomAt(target), from, direction, loopRandom, settings);
      walker.moveTo(target, direction);

      return false;
    }

    RoomNode created = graph.place(RoomShape.single(), target);

    graph.connect(currentRoom, created, new RoomEdge(from, direction), DoorType.NORMAL);
    walker.moveTo(target, direction);

    return true;
  }

  private void crossIntoExisting(FloorGraph graph, RoomNode currentRoom, RoomNode targetRoom, RoomCell from,
                                 Direction direction, Random loopRandom, WalkerSettings settings) {
    if (currentRoom == targetRoom || currentRoom.isLinkedTo(targetRoom)) {
      return;
    }

    if (loopRandom.nextDouble() < settings.loopChance()) {
      graph.connect(currentRoom, targetRoom, new RoomEdge(from, direction), DoorType.NORMAL);
    }
  }

  public enum StopReason {

    BUDGET_REACHED,
    BARREN,
    STEP_CAP,
    BLOCKED
  }

  public record WalkResult(FloorGraph graph, int steps, int loops, StopReason reason) {

    public int rooms() {
      return graph.size();
    }
  }
}
