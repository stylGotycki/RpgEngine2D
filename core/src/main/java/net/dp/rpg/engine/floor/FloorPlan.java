package net.dp.rpg.engine.floor;

import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapePools;
import net.dp.rpg.engine.floor.type.DefaultRoomTypes;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;

import java.util.List;

public record FloorPlan(
    int roomCount,
    double trunkRatio,
    double cellsPerRoom,
    ShapePool defaultShapes,
    List<RoomTypeDefinition> roomTypes,
    boolean exclusiveGroups) {

  public static final double DEFAULT_TRUNK_RATIO = 0.70;

  public static final double DEFAULT_CELLS_PER_ROOM = 1.8;

  public FloorPlan {
    if (roomCount < 2) {
      throw new IllegalArgumentException("A floor needs at least two rooms, got " + roomCount);
    }

    if (trunkRatio <= 0.0 || trunkRatio > 1.0) {
      throw new IllegalArgumentException("trunkRatio must be within 0..1, got " + trunkRatio);
    }

    if (cellsPerRoom < 1.0) {
      throw new IllegalArgumentException("cellsPerRoom must be at least 1, got " + cellsPerRoom);
    }

    roomTypes = List.copyOf(roomTypes);
  }

  public static FloorPlan of(int roomCount) {
    return new FloorPlan(roomCount, DEFAULT_TRUNK_RATIO, DEFAULT_CELLS_PER_ROOM,
        ShapePools.STANDARD, DefaultRoomTypes.catalog(), true);
  }

  public FloorPlan withTrunkRatio(double ratio) {
    return new FloorPlan(roomCount, ratio, cellsPerRoom, defaultShapes, roomTypes, exclusiveGroups);
  }

  public FloorPlan withCellsPerRoom(double cells) {
    return new FloorPlan(roomCount, trunkRatio, cells, defaultShapes, roomTypes, exclusiveGroups);
  }

  public FloorPlan withRoomTypes(List<RoomTypeDefinition> types) {
    return new FloorPlan(roomCount, trunkRatio, cellsPerRoom, defaultShapes, types, exclusiveGroups);
  }

  public int trunkRooms() {
    return Math.max(1, Math.min(roomCount - 1, (int) Math.round(roomCount * trunkRatio)));
  }

  public int appendixRooms() {
    return roomCount - trunkRooms();
  }

  public int cellBudget() {
    return (int) Math.round(roomCount * cellsPerRoom);
  }

  public int trunkCellBudget() {
    return cellBudget() - appendixRooms();
  }
}
