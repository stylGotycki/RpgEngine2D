package net.dp.rpg.engine.floor;

import java.util.List;
import java.util.Random;
import net.dp.rpg.engine.floor.phase.WalkerSettings;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.shape.ShapePools;
import net.dp.rpg.engine.floor.type.DefaultRoomTypes;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;

public record FloorArchetype(
    String id,
    int minRooms,
    int maxRooms,
    double density,
    double aspect,
    double trunkRatio,
    double cellsPerRoom,
    double criticalShare,
    LoopSettings loops,
    ShapePool defaultShapes,
    List<RoomTypeDefinition> roomTypes,
    QuestSettings quests,
    WalkerSettings walker,
    Postconditions postconditions,
    String tilesetId,
    boolean exclusiveGroups) {

  public FloorArchetype {
    if (minRooms < 2 || maxRooms < minRooms) {
      throw new IllegalArgumentException("Room range must be sane, got %d..%d".formatted(minRooms, maxRooms));
    }

    if (density <= 0.0 || density > 1.0) {
      throw new IllegalArgumentException("density must be within 0..1, got " + density);
    }

    if (aspect <= 0.0) {
      throw new IllegalArgumentException("aspect must be greater than zero, got " + aspect);
    }

    if (criticalShare <= 0.0 || criticalShare > 1.0) {
      throw new IllegalArgumentException("criticalShare must be within 0..1, got " + criticalShare);
    }

    if (loops == null) {
      throw new IllegalArgumentException("Loop settings must not be null");
    }

    roomTypes = List.copyOf(roomTypes);
  }

  public static FloorArchetype of(String id, int minRooms, int maxRooms) {
    return new FloorArchetype(id, minRooms, maxRooms, 0.35, 4.0 / 3.0,
        FloorPlan.DEFAULT_TRUNK_RATIO, FloorPlan.DEFAULT_CELLS_PER_ROOM,
        FloorPlan.DEFAULT_CRITICAL_SHARE, LoopSettings.defaults(), ShapePools.STANDARD,
        DefaultRoomTypes.catalog(), QuestSettings.defaults(), WalkerSettings.defaults(),
        Postconditions.defaults(), "terrain", true);
  }

  public FloorArchetype withDensity(double value) {
    return new FloorArchetype(id, minRooms, maxRooms, value, aspect, trunkRatio, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, quests, walker, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withTrunkRatio(double value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, value, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, quests, walker, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withCriticalShare(double value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        value, loops, defaultShapes, roomTypes, quests, walker, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withLoops(LoopSettings value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        criticalShare, value, defaultShapes, roomTypes, quests, walker, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withQuests(QuestSettings value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, value, walker, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withWalker(WalkerSettings value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, quests, value, postconditions, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withPostconditions(Postconditions value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, quests, walker, value, tilesetId,
        exclusiveGroups);
  }

  public FloorArchetype withTileset(String value) {
    return new FloorArchetype(id, minRooms, maxRooms, density, aspect, trunkRatio, cellsPerRoom,
        criticalShare, loops, defaultShapes, roomTypes, quests, walker, postconditions, value,
        exclusiveGroups);
  }

  public FloorArchetype with(FloorSettings settings) {
    return new FloorArchetype(id, settings.minRooms(), settings.maxRooms(), density, aspect,
        settings.trunkRatio(), cellsPerRoom, settings.criticalShare(), settings.loops(),
        defaultShapes, roomTypes, quests.withRoomRatio(settings.questRoomRatio()), walker,
        postconditions, tilesetId, exclusiveGroups);
  }

  public int rollRoomCount(Random random) {
    return minRooms + random.nextInt(maxRooms - minRooms + 1);
  }

  public FloorPlan planFor(int roomCount) {
    return new FloorPlan(id, roomCount, trunkRatio, cellsPerRoom, criticalShare, loops,
        defaultShapes, roomTypes, quests, exclusiveGroups);
  }

  public GridBounds boundsFor(int roomCount) {
    double area = roomCount * cellsPerRoom / density;
    int width = Math.max(3, (int) Math.ceil(Math.sqrt(area * aspect)));
    int height = Math.max(3, (int) Math.ceil(area / width));

    return new GridBounds(width, height);
  }
}
