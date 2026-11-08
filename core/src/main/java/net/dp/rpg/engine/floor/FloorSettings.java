package net.dp.rpg.engine.floor;

public record FloorSettings(
    int minRooms,
    int maxRooms,
    double trunkRatio,
    double criticalShare,
    LoopSettings loops,
    double questRoomRatio) {

  public FloorSettings {
    if (minRooms < 2 || maxRooms < minRooms) {
      throw new IllegalArgumentException("Room range must be sane, got %d..%d".formatted(minRooms,
          maxRooms));
    }

    if (trunkRatio <= 0.0 || trunkRatio > 1.0) {
      throw new IllegalArgumentException("trunkRatio must be within 0..1, got " + trunkRatio);
    }

    if (criticalShare <= 0.0 || criticalShare > 1.0) {
      throw new IllegalArgumentException("criticalShare must be within 0..1, got " + criticalShare);
    }

    if (loops == null) {
      throw new IllegalArgumentException("Loop settings must not be null");
    }

    if (questRoomRatio < 0.0 || questRoomRatio > 1.0) {
      throw new IllegalArgumentException("questRoomRatio must be within 0..1, got "
          + questRoomRatio);
    }
  }

  /** The archetype's own values: generating with these changes nothing. */
  public static FloorSettings of(FloorArchetype archetype) {
    return new FloorSettings(archetype.minRooms(), archetype.maxRooms(), archetype.trunkRatio(),
        archetype.criticalShare(), archetype.loops(), archetype.quests().roomRatio());
  }

  public FloorSettings withRooms(int min, int max) {
    return new FloorSettings(min, max, trunkRatio, criticalShare, loops, questRoomRatio);
  }

  public FloorSettings withTrunkRatio(double value) {
    return new FloorSettings(minRooms, maxRooms, value, criticalShare, loops, questRoomRatio);
  }

  public FloorSettings withCriticalShare(double value) {
    return new FloorSettings(minRooms, maxRooms, trunkRatio, value, loops, questRoomRatio);
  }

  public FloorSettings withLoops(LoopSettings value) {
    return new FloorSettings(minRooms, maxRooms, trunkRatio, criticalShare, value, questRoomRatio);
  }

  public FloorSettings withQuestRoomRatio(double value) {
    return new FloorSettings(minRooms, maxRooms, trunkRatio, criticalShare, loops, value);
  }
}
