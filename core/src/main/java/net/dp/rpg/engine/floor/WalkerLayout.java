package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class WalkerLayout {

  private static final int SHAPE_RETRIES = 6;

  public WalkResult grow(TrunkPlan plan, GridBounds bounds, WalkerSettings settings, long seed) {
    Random layoutRandom = RandomSource.derive(seed, "layout");
    Random shapeRandom = RandomSource.derive(seed, "shape");
    Random loopRandom = RandomSource.derive(seed, "loop");

    FloorGraph graph = new FloorGraph();
    ShapeDrawContext shapes = new ShapeDrawContext(plan.exclusiveGroups());

    placeStart(graph, plan, shapes, bounds, shapeRandom);

    Walker walker = new Walker(bounds.center(), bounds, settings);

    int steps = 0;
    int barrenSteps = 0;
    StopReason reason = null;

    while (reason == null) {
      if (graph.size() >= plan.roomBudget()) {
        reason = StopReason.BUDGET_REACHED;
      } else if (barrenSteps >= settings.maxBarrenSteps()) {
        reason = StopReason.BARREN;
      } else if (steps >= settings.hardStepCap()) {
        reason = StopReason.STEP_CAP;
      } else {
        Outcome outcome = attemptStep(graph, walker, plan, shapes, bounds, settings,
            layoutRandom, shapeRandom, loopRandom);

        if (outcome == Outcome.BLOCKED) {
          reason = StopReason.BLOCKED;
        } else {
          steps++;
          barrenSteps = outcome == Outcome.CREATED ? 0 : barrenSteps + 1;
        }
      }
    }

    int loops = closeLoops(graph, loopRandom, settings);

    return new WalkResult(graph, steps, loops, reason);
  }

  private void placeStart(FloorGraph graph, TrunkPlan plan, ShapeDrawContext shapes, GridBounds bounds,
                          Random shapeRandom) {
    RoomCell centre = bounds.center();

    for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
      ShapeVariant variant = drawShape(graph, plan, shapes, shapeRandom);
      RoomCell origin = fittingOrigin(graph, variant, centre, bounds, shapeRandom);

      if (origin != null) {
        place(graph, variant, origin);
        shapes.confirm(variant);

        return;
      }
    }

    place(graph, Shapes.SINGLE.firstVariant(), centre);
  }

  private Outcome attemptStep(FloorGraph graph, Walker walker, TrunkPlan plan, ShapeDrawContext shapes,
                              GridBounds bounds, WalkerSettings settings, Random layoutRandom, Random shapeRandom, Random loopRandom) {
    Direction excluded = null;

    for (int attempt = 0; attempt < 2; attempt++) {
      Direction direction = walker.chooseDirection(graph, layoutRandom, excluded);

      if (direction == null) {
        return Outcome.BLOCKED;
      }

      Outcome outcome = step(graph, walker, direction, plan, shapes, bounds, settings, shapeRandom, loopRandom);

      if (outcome != Outcome.NO_FIT) {
        return outcome;
      }

      excluded = direction;
    }

    return Outcome.NO_FIT;
  }

  private Outcome step(FloorGraph graph, Walker walker, Direction direction, TrunkPlan plan,
                       ShapeDrawContext shapes, GridBounds bounds, WalkerSettings settings, Random shapeRandom,
                       Random loopRandom) {
    RoomCell from = walker.cell();
    RoomCell target = from.neighbour(direction);
    RoomNode currentRoom = graph.roomAt(from);

    if (graph.isOccupied(target)) {
      crossIntoExisting(graph, currentRoom, graph.roomAt(target), from, direction, loopRandom, settings);
      walker.moveTo(target, direction);

      return Outcome.MOVED;
    }

    for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
      ShapeVariant variant = drawShape(graph, plan, shapes, shapeRandom);
      RoomCell origin = fittingOrigin(graph, variant, target, bounds, shapeRandom);

      if (origin == null) {
        continue;
      }

      RoomNode created = place(graph, variant, origin);

      shapes.confirm(variant);
      graph.connect(currentRoom, created, new RoomEdge(from, direction), DoorType.NORMAL);
      walker.moveTo(target, direction);

      return Outcome.CREATED;
    }

    return Outcome.NO_FIT;
  }

  private RoomCell fittingOrigin(FloorGraph graph, ShapeVariant variant, RoomCell target, GridBounds bounds,
                                 Random shapeRandom) {
    List<RoomCell> anchors = new ArrayList<>(variant.shape().cells());

    Collections.shuffle(anchors, shapeRandom);

    for (RoomCell anchor : anchors) {
      RoomCell origin = new RoomCell(target.x() - anchor.x(), target.y() - anchor.y());

      if (graph.canPlace(variant, origin, bounds)) {
        return origin;
      }
    }

    return null;
  }

  private RoomNode place(FloorGraph graph, ShapeVariant variant, RoomCell origin) {
    RoomNode room = graph.place(variant, origin);

    variant.holeCellsAt(origin).forEach(graph::reserve);

    return room;
  }

  private ShapeVariant drawShape(FloorGraph graph, TrunkPlan plan, ShapeDrawContext shapes, Random shapeRandom) {
    int cellsLeft = plan.cellBudget() - graph.budgetedCells();
    int roomsLeft = Math.max(1, plan.roomBudget() - graph.size());
    double factor = ShapeDrawContext.budgetFactor(plan.cellBudget(), plan.roomBudget(),
        graph.budgetedCells(), graph.size());

    return shapes.draw(plan.shapes(), roomsLeft, cellsLeft, factor, shapeRandom);
  }

  private int closeLoops(FloorGraph graph, Random loopRandom, WalkerSettings settings) {
    int opened = 0;

    for (RoomNode room : graph.rooms()) {
      for (RoomEdge edge : room.outerEdges()) {
        RoomNode other = graph.roomAt(edge.cell().neighbour(edge.direction()));

        boolean candidate = other != null && other.index() > room.index() && !room.isLinkedTo(other);

        if (candidate && loopRandom.nextDouble() < settings.loopChance()) {
          graph.connect(room, other, edge, DoorType.NORMAL);
          opened++;
        }
      }
    }

    return opened;
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

  private enum Outcome {

    CREATED,
    MOVED,
    NO_FIT,
    BLOCKED
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
