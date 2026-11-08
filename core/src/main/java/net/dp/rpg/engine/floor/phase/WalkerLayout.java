package net.dp.rpg.engine.floor.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.dp.rpg.engine.floor.FloorContext;
import net.dp.rpg.engine.floor.FloorPlan;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.Shapes;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.shape.ShapeDrawContext;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.AbilityPhase;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class WalkerLayout {

  private static final int SHAPE_RETRIES = 6;

  public WalkResult grow(FloorContext context) {
    FloorPlan plan = context.plan();
    GridBounds bounds = context.bounds();
    WalkerSettings settings = context.settings();

    Random layoutRandom = context.stream("layout");
    Random shapeRandom = context.stream("shape");
    Random typeRandom = context.stream("type");
    Random doorRandom = context.stream("door");

    FloorGraph graph = new FloorGraph();
    ShapeDrawContext shapes = new ShapeDrawContext(plan.exclusiveGroups());

    placeStart(graph, context, shapes, shapeRandom);

    Walker walker = new Walker(bounds.center(), bounds, settings);
    List<RoomCell> path = new ArrayList<>();

    path.add(walker.cell());

    int steps = 0;
    int barrenSteps = 0;
    StopReason reason = null;

    while (reason == null) {
      if (graph.size() >= plan.trunkRooms()) {
        reason = StopReason.BUDGET_REACHED;
      } else if (barrenSteps >= settings.maxBarrenSteps()) {
        reason = StopReason.BARREN;
      } else if (steps >= settings.hardStepCap()) {
        reason = StopReason.STEP_CAP;
      } else {
        Outcome outcome = attemptStep(graph, walker, context, shapes, layoutRandom, shapeRandom, typeRandom);

        if (outcome == Outcome.BLOCKED) {
          reason = StopReason.BLOCKED;
        } else {
          steps++;
          barrenSteps = outcome == Outcome.CREATED ? 0 : barrenSteps + 1;

          if (outcome != Outcome.NO_FIT) {
            path.add(walker.cell());
          }
        }
      }
    }

    int extraDoors = connectTouchingRooms(graph, doorRandom, settings);

    return new WalkResult(graph, graph.size(), steps, extraDoors, List.copyOf(path), reason);
  }

  private void placeStart(FloorGraph graph, FloorContext context, ShapeDrawContext shapes, Random shapeRandom) {
    RoomCell centre = context.bounds().center();
    ShapePool pool = context.types().poolFor(RoomType.START);

    for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
      ShapeVariant variant = drawShape(graph, context, pool, shapes, shapeRandom);
      RoomCell origin = fittingOrigin(graph, variant, centre, context.bounds(), shapeRandom);

      if (origin != null) {
        place(graph, variant, origin).setType(RoomType.START);
        shapes.confirm(variant);
        context.loadout().spend(AbilityPhase.TRUNK, RoomType.START);

        return;
      }
    }

    place(graph, Shapes.SINGLE.firstVariant(), centre).setType(RoomType.START);
    context.loadout().spend(AbilityPhase.TRUNK, RoomType.START);
  }

  private Outcome attemptStep(FloorGraph graph, Walker walker, FloorContext context, ShapeDrawContext shapes,
                              Random layoutRandom, Random shapeRandom, Random typeRandom) {
    Direction excluded = null;

    for (int attempt = 0; attempt < 2; attempt++) {
      Direction direction = walker.chooseDirection(graph, layoutRandom, excluded);

      if (direction == null) {
        return Outcome.BLOCKED;
      }

      Outcome outcome = step(graph, walker, direction, context, shapes, shapeRandom, typeRandom);

      if (outcome != Outcome.NO_FIT) {
        return outcome;
      }

      excluded = direction;
    }

    return Outcome.NO_FIT;
  }

  private Outcome step(FloorGraph graph, Walker walker, Direction direction, FloorContext context,
                       ShapeDrawContext shapes, Random shapeRandom, Random typeRandom) {
    RoomCell from = walker.cell();
    RoomCell target = from.neighbour(direction);
    RoomNode currentRoom = graph.roomAt(from);

    if (graph.isOccupied(target)) {
      walker.moveTo(target, direction);

      return Outcome.MOVED;
    }

    int opportunities = Math.max(1, context.plan().trunkRooms() - graph.size());
    RoomType type = context.loadout().next(AbilityPhase.TRUNK, opportunities, typeRandom);
    ShapePool pool = context.types().poolFor(type);

    for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
      ShapeVariant variant = drawShape(graph, context, pool, shapes, shapeRandom);
      RoomCell origin = fittingOrigin(graph, variant, target, context.bounds(), shapeRandom);

      if (origin == null) {
        continue;
      }

      RoomNode created = place(graph, variant, origin);

      created.setType(type);
      shapes.confirm(variant);
      context.loadout().spend(AbilityPhase.TRUNK, type);
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

  private ShapeVariant drawShape(FloorGraph graph, FloorContext context, ShapePool pool, ShapeDrawContext shapes,
                                 Random shapeRandom) {
    FloorPlan plan = context.plan();
    int cellsLeft = plan.trunkCellBudget() - graph.budgetedCells();
    int roomsLeft = Math.max(1, plan.trunkRooms() - graph.size());
    double factor = ShapeDrawContext.budgetFactor(plan.trunkCellBudget(), plan.trunkRooms(),
        graph.budgetedCells(), graph.size());

    return shapes.draw(pool, roomsLeft, cellsLeft, factor, shapeRandom);
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

  public record WalkResult(FloorGraph graph, int rooms, int steps, int extraDoors, List<RoomCell> path,
                           StopReason reason) {

    public WalkResult {
      path = List.copyOf(path);
    }
  }
}
