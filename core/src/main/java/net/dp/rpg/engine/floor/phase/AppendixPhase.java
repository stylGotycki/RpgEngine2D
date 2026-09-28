package net.dp.rpg.engine.floor.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.dp.rpg.engine.floor.*;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.graph.RoomPhase;
import net.dp.rpg.engine.floor.shape.ShapeDrawContext;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.AbilityPhase;
import net.dp.rpg.engine.floor.type.Loadout;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.floor.type.SlotPreference;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class AppendixPhase {

  private static final int SHAPE_RETRIES = 6;

  private static final double DEPTH_WEIGHT = 1.0;

  private static final int ANCHOR_RETRIES = 10;

  public Result attach(FloorGraph graph, FloorContext context, ShapeDrawContext shapes) {
    Random anchorRandom = context.stream("anchor");
    Random shapeRandom = context.stream("appendix");
    Random typeRandom = context.stream("appendixType");

    int wanted = context.plan().appendixRooms();
    int attached = 0;

    for (int index = 0; index < wanted - 1; index++) {
      int opportunities = Math.max(1, wanted - 1 - index);
      RoomType type = context.loadout().next(AbilityPhase.APPENDIX, opportunities, typeRandom);

      if (attachOne(graph, context, type, shapes, anchorRandom, shapeRandom) != null) {
        attached++;
      }
    }

    attached += spendRemaining(graph, context, shapes, anchorRandom, shapeRandom);

    RoomNode boss = attachBoss(graph, context, shapes, shapeRandom);

    return new Result(attached, boss);
  }

  private int spendRemaining(FloorGraph graph, FloorContext context, ShapeDrawContext shapes,
                             Random anchorRandom, Random shapeRandom) {
    int attached = 0;

    for (Loadout.Charge charge : context.loadout().charges(AbilityPhase.APPENDIX)) {
      boolean structural = charge.definition().slot() == SlotPreference.CRITICAL_TERMINAL;

      while (charge.mandatory() && charge.remaining() > 0 && !structural) {
        if (attachOne(graph, context, charge.type(), shapes, anchorRandom, shapeRandom) == null) {
          break;
        }

        attached++;
      }
    }

    return attached;
  }

  private RoomNode attachOne(FloorGraph graph, FloorContext context, RoomType type, ShapeDrawContext shapes,
                             Random anchorRandom, Random shapeRandom) {
    ShapePool pool = context.types().poolFor(type);

    for (int retry = 0; retry < ANCHOR_RETRIES; retry++) {
      RoomNode anchor = pickAnchor(graph, context.bounds(), anchorRandom);

      if (anchor == null) {
        return null;
      }

      RoomNode room = attach(graph, anchor, pool, context, shapes, shapeRandom);

      if (room != null) {
        room.setType(type);
        context.loadout().spend(AbilityPhase.APPENDIX, type);

        return room;
      }
    }

    return null;
  }

  private RoomNode attachBoss(FloorGraph graph, FloorContext context, ShapeDrawContext shapes,
                              Random shapeRandom) {
    if (!context.loadout().hasCharge(AbilityPhase.APPENDIX, RoomType.BOSS)) {
      return null;
    }

    graph.computeDepths();

    ShapePool pool = context.types().poolFor(RoomType.BOSS);
    List<RoomNode> byDepth = new ArrayList<>(graph.rooms());

    byDepth.sort((first, second) -> Integer.compare(second.depth(), first.depth()));

    for (RoomNode anchor : byDepth) {
      RoomNode boss = attach(graph, anchor, pool, context, shapes, shapeRandom);

      if (boss != null) {
        boss.setType(RoomType.BOSS);
        graph.markExempt(boss);
        context.loadout().spend(AbilityPhase.APPENDIX, RoomType.BOSS);

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

  private RoomNode attach(FloorGraph graph, RoomNode anchor, ShapePool pool, FloorContext context,
                          ShapeDrawContext shapes, Random shapeRandom) {
    List<RoomEdge> openings = freeNeighbourCells(graph, anchor, context.bounds());

    Collections.shuffle(openings, shapeRandom);

    for (RoomEdge opening : openings) {
      RoomCell target = opening.cell().neighbour(opening.direction());

      for (int retry = 0; retry < SHAPE_RETRIES; retry++) {
        ShapeVariant variant = draw(graph, context, pool, shapes, shapeRandom);
        RoomCell origin = fittingOrigin(graph, variant, target, anchor, context.bounds(), shapeRandom);

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

  private ShapeVariant draw(FloorGraph graph, FloorContext context, ShapePool pool, ShapeDrawContext shapes,
                            Random shapeRandom) {
    FloorPlan plan = context.plan();
    int cellsLeft = plan.cellBudget() - graph.budgetedCells();
    int roomsLeft = Math.max(1, plan.roomCount() - graph.size());
    double factor = ShapeDrawContext.budgetFactor(plan.cellBudget(), plan.roomCount(),
        graph.budgetedCells(), graph.size());

    return shapes.draw(pool, roomsLeft, cellsLeft, factor, shapeRandom);
  }

  public record Result(int attached, RoomNode boss) {
  }
}
