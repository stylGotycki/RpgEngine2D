package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class HolePhase {

  public int fill(FloorGraph graph, FloorContext context) {
    Random holeRandom = context.stream("hole");
    Random shapeRandom = context.stream("holeShape");
    ShapePool pool = context.types().poolFor(RoomType.VAULT);
    int filled = 0;

    for (RoomNode host : List.copyOf(graph.rooms())) {
      if (!host.variant().hasHoles()) {
        continue;
      }

      double chance = host.variant().definition().holeRoomChance();

      for (List<RoomCell> region : host.variant().holeRegions()) {
        List<RoomCell> cells = toFloorCells(region, host);

        cells.forEach(graph::release);

        if (holeRandom.nextDouble() >= chance) {
          continue;
        }

        if (placeInside(graph, host, cells, pool, context.bounds(), shapeRandom) != null) {
          filled++;
        }
      }
    }

    return filled;
  }

  private RoomNode placeInside(FloorGraph graph, RoomNode host, List<RoomCell> region, ShapePool pool,
                               GridBounds bounds, Random shapeRandom) {
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

            if (!fitsInside(variant, origin, allowed) || !graph.canPlace(variant, origin, bounds)) {
              continue;
            }

            RoomNode inner = graph.place(variant, origin);

            inner.setPhase(RoomPhase.HOLE);
            inner.setType(RoomType.VAULT);
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
