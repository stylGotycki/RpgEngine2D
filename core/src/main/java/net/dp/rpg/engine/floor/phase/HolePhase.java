package net.dp.rpg.engine.floor.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorContext;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.graph.RoomPhase;
import net.dp.rpg.engine.floor.shape.RoomShapeDef;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.AbilityPhase;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class HolePhase {

  public int fill(FloorGraph graph, FloorContext context) {
    Random holeRandom = context.stream("hole");
    Random shapeRandom = context.stream("holeShape");
    Random typeRandom = context.stream("holeType");

    List<RoomNode> hosts = hostsWithHoles(graph);
    int remaining = countRegions(hosts);
    int filled = 0;

    for (RoomNode host : hosts) {
      ShapePool hostPool = context.types().poolFor(host.type());
      double chance = hostPool.holeRoomChanceOf(host.variant().definition());

      for (List<RoomCell> region : host.variant().holeRegions()) {
        List<RoomCell> cells = toFloorCells(region, host);

        cells.forEach(graph::release);
        remaining--;

        if (holeRandom.nextDouble() >= chance) {
          continue;
        }

        RoomType type = context.loadout().next(AbilityPhase.HOLE, Math.max(1, remaining),
            RoomType.VAULT, typeRandom);
        ShapePool pool = context.types().poolFor(type);

        if (placeInside(graph, host, cells, type, pool, context, shapeRandom) != null) {
          context.loadout().spend(AbilityPhase.HOLE, type);
          filled++;
        }
      }
    }

    return filled;
  }

  private List<RoomNode> hostsWithHoles(FloorGraph graph) {
    List<RoomNode> hosts = new ArrayList<>();

    graph.rooms().stream().filter(room -> room.variant().hasHoles()).forEach(hosts::add);

    return hosts;
  }

  private int countRegions(List<RoomNode> hosts) {
    return hosts.stream().mapToInt(host -> host.variant().holeRegions().size()).sum();
  }

  private RoomNode placeInside(FloorGraph graph, RoomNode host, List<RoomCell> region, RoomType type,
                               ShapePool pool, FloorContext context, Random shapeRandom) {
    Set<RoomCell> allowed = new LinkedHashSet<>(region);
    List<RoomShapeDef> candidates = new ArrayList<>();

    pool.entries().stream()
        .map(ShapePool.ShapeEntry::shape)
        .filter(shape -> shape.size() <= region.size())
        .forEach(candidates::add);
    Collections.shuffle(candidates, shapeRandom);
    candidates.sort((first, second) -> Integer.compare(second.size(), first.size()));

    for (RoomShapeDef definition : candidates) {
      for (ShapeVariant variant : definition.variants()) {
        for (RoomCell target : region) {
          for (RoomCell anchor : variant.shape().cells()) {
            RoomCell origin = new RoomCell(target.x() - anchor.x(), target.y() - anchor.y());

            if (!fitsInside(variant, origin, allowed)
                || !graph.canPlace(variant, origin, context.bounds())) {
              continue;
            }

            RoomNode inner = graph.place(variant, origin);

            inner.setPhase(RoomPhase.HOLE);
            inner.setType(type);
            graph.markExempt(inner);
            graph.connect(host, inner, hostEdgeTo(host, inner), DoorType.LOCKED);

            return inner;
          }
        }
      }
    }

    return null;
  }

  private boolean fitsInside(ShapeVariant variant, RoomCell origin, Set<RoomCell> allowed) {
    for (RoomCell cell : variant.shape().cells()) {
      if (!allowed.contains(cell.translated(origin.x(), origin.y()))) {
        return false;
      }
    }

    return true;
  }

  private RoomEdge hostEdgeTo(RoomNode host, RoomNode inner) {
    for (RoomCell cell : host.cells()) {
      for (Direction direction : Direction.values()) {
        if (inner.contains(cell.neighbour(direction))) {
          return new RoomEdge(cell, direction);
        }
      }
    }

    throw new IllegalStateException("%s does not enclose %s".formatted(host, inner));
  }

  private List<RoomCell> toFloorCells(List<RoomCell> region, RoomNode host) {
    List<RoomCell> cells = new ArrayList<>();

    region.forEach(cell -> cells.add(cell.translated(host.origin().x(), host.origin().y())));

    return cells;
  }
}
