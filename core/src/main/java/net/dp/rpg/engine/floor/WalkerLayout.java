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
    Random doorRandom = RandomSource.derive(seed, "door");

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
        Outcome outcome = attemptStep(graph, walker, plan, shapes, bounds, layoutRandom, shapeRandom);

        if (outcome == Outcome.BLOCKED) {
          reason = StopReason.BLOCKED;
        } else {
          steps++;
          barrenSteps = outcome == Outcome.CREATED ? 0 : barrenSteps + 1;
        }
      }
    }

    int extraDoors = connectTouchingRooms(graph, doorRandom, settings);

    return new WalkResult(graph, steps, extraDoors, reason);
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
                              GridBounds bounds, Random layoutRandom, Random shapeRandom) {
    Direction excluded = null;

    for (int attempt = 0; attempt < 2; attempt++) {
      Direction direction = walker.chooseDirection(graph, layoutRandom, excluded);

      if (direction == null) {
        return Outcome.BLOCKED;
      }

      Outcome outcome = step(graph, walker, direction, plan, shapes, bounds, shapeRandom);

      if (outcome != Outcome.NO_FIT) {
        return outcome;
      }

      excluded = direction;
    }

    return Outcome.NO_FIT;
  }

  private Outcome step(FloorGraph graph, Walker walker, Direction direction, TrunkPlan plan,
                       ShapeDrawContext shapes, GridBounds bounds, Random shapeRandom) {
    RoomCell from = walker.cell();
    RoomCell target = from.neighbour(direction);
    RoomNode currentRoom = graph.roomAt(from);

    if (graph.isOccupied(target)) {
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

  private int connectTouchingRooms(FloorGraph graph, Random doorRandom, WalkerSettings settings) {
    int opened = 0;

    for (RoomNode room : graph.rooms()) {
      for (RoomEdge edge : room.outerEdges()) {
        RoomNode other = graph.roomAt(edge.cell().neighbour(edge.direction()));

        if (other == null || other.index() < room.index()) {
          continue;
        }

        if (graph.doorAt(edge.cell(), edge.direction()) != null) {
          continue;
        }

        if (doorRandom.nextDouble() < settings.extraDoorChance()) {
          graph.connect(room, other, edge, DoorType.NORMAL);
          opened++;
        }
      }
    }

    return opened;
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

  public record WalkResult(FloorGraph graph, int steps, int extraDoors, StopReason reason) {

    public int rooms() {
      return graph.size();
    }
  }
}
