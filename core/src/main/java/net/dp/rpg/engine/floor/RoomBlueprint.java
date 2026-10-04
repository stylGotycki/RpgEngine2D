package net.dp.rpg.engine.floor;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public record RoomBlueprint(
    RoomCell origin,
    ShapeVariant variant,
    RoomType type,
    Map<RoomEdge, DoorType> doors,
    String questId,
    int depth,
    long seed) {

  public static final Comparator<RoomCell> CELL_ORDER =
      Comparator.comparingInt(RoomCell::y).thenComparingInt(RoomCell::x);

  public static final Comparator<RoomEdge> EDGE_ORDER =
      Comparator.comparing(RoomEdge::cell, CELL_ORDER).thenComparing(RoomEdge::direction);

  public RoomBlueprint {
    requireNonNull(origin, "origin");
    requireNonNull(variant, "variant");
    requireNonNull(type, "type");
    requireNonNull(doors, "doors");

    doors = canonical(doors);
    requireOuterEdges(variant.shape(), doors);
  }

  public static RoomBlueprint of(RoomNode room, long floorSeed) {
    return new RoomBlueprint(room.origin(), room.variant(), room.type(), room.localDoors(),
        room.questId(), room.depth(), seedOf(floorSeed, room.anchor()));
  }

  public static long seedOf(long floorSeed, RoomCell anchor) {
    return RandomSource.deriveSeed(floorSeed, "room:" + anchor.x() + "," + anchor.y());
  }

  /** The same room with another seed: a retry or a reroll of its interior, nothing else changes. */
  public RoomBlueprint withSeed(long newSeed) {
    return new RoomBlueprint(origin, variant, type, doors, questId, depth, newSeed);
  }

  public RoomCell anchor() {
    RoomCell topLeft = variant.shape().topLeftCell();

    return origin.translated(topLeft.x(), topLeft.y());
  }

  public RoomShape shape() {
    return variant.shape();
  }

  public boolean hasDoor(RoomEdge edge) {
    return doors.containsKey(edge);
  }

  public DoorType doorAt(RoomEdge edge) {
    return doors.get(edge);
  }

  private static Map<RoomEdge, DoorType> canonical(Map<RoomEdge, DoorType> doors) {
    Map<RoomEdge, DoorType> sorted = new TreeMap<>(EDGE_ORDER);

    doors.forEach((edge, doorType) -> {
      requireNonNull(edge, "door edge");
      requireNonNull(doorType, "door type at " + edge);
      sorted.put(edge, doorType);
    });

    return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
  }

  private static void requireOuterEdges(RoomShape shape, Map<RoomEdge, DoorType> doors) {
    for (RoomEdge edge : doors.keySet()) {
      boolean inside = shape.contains(edge.cell());
      boolean facesOut = !shape.contains(edge.cell().neighbour(edge.direction()));

      if (!inside || !facesOut) {
        throw new IllegalArgumentException(
            "Door %s is not an outer edge of the room shape; is it in floor cells instead of local?"
                .formatted(edge));
      }
    }
  }

  private static void requireNonNull(Object value, String field) {
    if (value == null) {
      throw new IllegalArgumentException("Room blueprint " + field + " must not be null");
    }
  }
}
