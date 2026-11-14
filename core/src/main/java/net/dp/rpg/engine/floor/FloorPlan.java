package net.dp.rpg.engine.floor;

import java.util.List;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapePools;
import net.dp.rpg.engine.floor.type.DefaultRoomTypes;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;

public record FloorPlan(
    String archetypeId,
    int roomCount,
    double trunkRatio,
    double cellsPerRoom,
    double criticalShare,
    LoopSettings loops,
    ShapePool defaultShapes,
    List<RoomTypeDefinition> roomTypes,
    QuestSettings questSettings,
    boolean exclusiveGroups,
    BranchSettings branches) {

  public static final double DEFAULT_TRUNK_RATIO = 0.70;

  public static final double DEFAULT_CELLS_PER_ROOM = 1.8;

  public static final double DEFAULT_CRITICAL_SHARE = 0.5;

  public static final int MIN_CRITICAL_ROOMS = 3;

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

    if (criticalShare <= 0.0 || criticalShare > 1.0) {
      throw new IllegalArgumentException("criticalShare must be within 0..1, got " + criticalShare);
    }

    if (loops == null) {
      throw new IllegalArgumentException("Loop settings must not be null");
    }

    roomTypes = List.copyOf(roomTypes);
    branches = branches == null ? BranchSettings.NONE : branches;
  }

  public static FloorPlan of(int roomCount) {
    return new FloorPlan("default", roomCount, DEFAULT_TRUNK_RATIO, DEFAULT_CELLS_PER_ROOM,
        DEFAULT_CRITICAL_SHARE, LoopSettings.defaults(), ShapePools.STANDARD,
        DefaultRoomTypes.catalog(), QuestSettings.defaults(), true, BranchSettings.NONE);
  }

  public FloorPlan withTrunkRatio(double ratio) {
    return new FloorPlan(archetypeId, roomCount, ratio, cellsPerRoom, criticalShare, loops,
        defaultShapes, roomTypes, questSettings, exclusiveGroups, branches);
  }

  public FloorPlan withCellsPerRoom(double cells) {
    return new FloorPlan(archetypeId, roomCount, trunkRatio, cells, criticalShare, loops,
        defaultShapes, roomTypes, questSettings, exclusiveGroups, branches);
  }

  public FloorPlan withCriticalShare(double share) {
    return new FloorPlan(archetypeId, roomCount, trunkRatio, cellsPerRoom, share, loops,
        defaultShapes, roomTypes, questSettings, exclusiveGroups, branches);
  }

  public FloorPlan withLoops(LoopSettings value) {
    return new FloorPlan(archetypeId, roomCount, trunkRatio, cellsPerRoom, criticalShare, value,
        defaultShapes, roomTypes, questSettings, exclusiveGroups, branches);
  }

  public FloorPlan withQuestSettings(QuestSettings quests) {
    return new FloorPlan(archetypeId, roomCount, trunkRatio, cellsPerRoom, criticalShare, loops,
        defaultShapes, roomTypes, quests, exclusiveGroups, branches);
  }

  public FloorPlan withRoomTypes(List<RoomTypeDefinition> types) {
    return new FloorPlan(archetypeId, roomCount, trunkRatio, cellsPerRoom, criticalShare, loops,
        defaultShapes, types, questSettings, exclusiveGroups, branches);
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

  public int criticalRooms() {
    return Math.max(MIN_CRITICAL_ROOMS, (int) Math.round(roomCount * criticalShare));
  }

  public int targetTrunkDepth() {
    return Math.max(1, Math.min(trunkRooms() - 1, criticalRooms() - 2));
  }
}
