package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class AppendixPhase {

  private static final int SHAPE_RETRIES = 6;

  private static final double DEPTH_WEIGHT = 1.0;

  private static final int ANCHOR_RETRIES = 10;

  public Result attach(FloorGraph graph, FloorPlan plan, ShapeDrawContext shapes, GridBounds bounds,
                       Random anchorRandom, Random shapeRandom) {
    int wanted = plan.appendixRooms();
    int attached = 0;

    for (int index = 0; index < wanted - 1; index++) {
      if (attachOne(graph, plan, shapes, bounds, anchorRandom, shapeRandom) != null) {
        attached++;
      }
    }

    RoomNode boss = attachBoss(graph, plan, shapes, bounds, shapeRandom);

    return new Result(attached, boss);
  }

  private RoomNode attachOne(FloorGraph graph, FloorPlan plan, ShapeDrawContext shapes, GridBounds bounds,
                             Random anchorRandom, Random shapeRandom) {
    for (int retry = 0; retry < ANCHOR_RETRIES; retry++) {
      RoomNode anchor = pickAnchor(graph, bounds, anchorRandom);

      if (anchor == null) {
        return null;
      }

      RoomNode room = attach(graph, anchor, plan.defaultShapes(), plan, shapes, bounds, shapeRandom);

      if (room != null) {
        return room;
      }
    }

    return null;
  }

  private RoomNode attachBoss(FloorGraph graph, FloorPlan plan, ShapeDrawContext shapes, GridBounds bounds,
                              Random shapeRandom) {
    graph.computeDepths();

    List<RoomNode> byDepth = new ArrayList<>(graph.rooms());

    byDepth.sort((first, second) -> Integer.compare(second.depth(), first.depth()));

    for (RoomNode anchor : byDepth) {
      RoomNode boss = attach(graph, anchor, plan.bossShapes(), plan, shapes, bounds, shapeRandom);

      if (boss != null) {
        graph.markExempt(boss);

        return boss;
      }
    }

    return null;
  }

  private RoomNode pickAnchor(FloorGraph graph, GridBounds bounds, Random anchorRandom) {
    graph.computeDepths();

    List<RoomNode> candidates = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    double total = 0.0;

    for (RoomNode room : graph.rooms()) {
      if (freeNeighbourCells(graph, room, bounds).isEmpty()) {
        continue;
      }

      double weight = 1.0 + DEPTH_WEIGHT * Math.max(0, room.depth());

      candidates.add(room);
      weights.add(weight);
      total += weight;
    }

    if (candidates.isEmpty()) {
      return null;
    }

    double roll = anchorRandom.nextDouble() * total;

    for (int index = 0; index < candidates.size(); index++) {
      roll -= weights.get(index);

      if (roll <= 0.0) {
        return candidates.get(index);
      }
    }

    return candidates.get(candidates.size() - 1);
  }

  private RoomNode attach(FloorGraph graph, RoomNode anchor, ShapePool pool, FloorPlan plan,
                          ShapeDrawContext shapes, GridBounds bounds, Random shapeRandom) {
    List<RoomEdge> openings = freeNeighbourCells(graph, anchor, bounds);

    Collections.shuffle(openings, shapeRandom);

    for (RoomEdge opening : openings) {
      RoomCell target = opening.cell().neighbour(opening.direction());

      for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
        ShapeVariant variant = draw(graph, plan, pool, shapes, shapeRandom);
        RoomCell origin = fittingOrigin(graph, variant, target, anchor, bounds, shapeRandom);

        if (origin == null) {
          continue;
        }

        RoomNode room = graph.place(variant, origin);

        shapes.confirm(variant);
        variant.holeCellsAt(origin).forEach(graph::reserve);
        room.setPhase(RoomPhase.APPENDIX);
        graph.connect(anchor, room, opening, DoorType.NORMAL);

        return room;
      }
    }

    return null;
  }

  private RoomCell fittingOrigin(FloorGraph graph, ShapeVariant variant, RoomCell target, RoomNode anchor,
                                 GridBounds bounds, Random shapeRandom) {
    List<RoomCell> anchors = new ArrayList<>(variant.shape().cells());

    Collections.shuffle(anchors, shapeRandom);

    for (RoomCell cell : anchors) {
      RoomCell origin = new RoomCell(target.x() - cell.x(), target.y() - cell.y());

      if (!graph.canPlace(variant, origin, bounds)) {
        continue;
      }

      List<RoomCell> placed = new ArrayList<>();

      variant.shape().cells().forEach(c -> placed.add(c.translated(origin.x(), origin.y())));

      Set<RoomNode> touching = graph.touching(placed);

      if (touching.size() == 1 && touching.contains(anchor)) {
        return origin;
      }
    }

    return null;
  }

  private List<RoomEdge> freeNeighbourCells(FloorGraph graph, RoomNode room, GridBounds bounds) {
    List<RoomEdge> openings = new ArrayList<>();

    for (RoomCell cell : room.cells()) {
      for (Direction direction : Direction.values()) {
        RoomCell neighbour = cell.neighbour(direction);

        if (bounds.contains(neighbour) && graph.isFree(neighbour)) {
          openings.add(new RoomEdge(cell, direction));
        }
      }
    }

    return openings;
  }

  private ShapeVariant draw(FloorGraph graph, FloorPlan plan, ShapePool pool, ShapeDrawContext shapes,
                            Random shapeRandom) {
    int cellsLeft = plan.cellBudget() - graph.budgetedCells();
    int roomsLeft = Math.max(1, plan.roomCount() - graph.size());
    double factor = ShapeDrawContext.budgetFactor(plan.cellBudget(), plan.roomCount(),
        graph.budgetedCells(), graph.size());

    return shapes.draw(pool, roomsLeft, cellsLeft, factor, shapeRandom);
  }

  public record Result(int attached, RoomNode boss) {
  }
}
