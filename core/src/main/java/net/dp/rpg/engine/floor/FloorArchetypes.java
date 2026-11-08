package net.dp.rpg.engine.floor;

import java.util.List;

public final class FloorArchetypes {

  public static final FloorArchetype CAVES = FloorArchetype.of("caves", 16, 24)
      .withDensity(0.30)
      .withTrunkRatio(0.75)
      .withCriticalShare(0.55)
      .withLoops(new LoopSettings(0.10, 4, 0.1))
      .withTileset("terrain");

  public static final FloorArchetype CATACOMBS = FloorArchetype.of("catacombs", 18, 28)
      .withDensity(0.50)
      .withTrunkRatio(0.60)
      .withCriticalShare(0.45)
      .withLoops(new LoopSettings(0.20, 4, 0.1))
      .withTileset("basement");

  public static final List<FloorArchetype> ALL = List.of(CAVES, CATACOMBS);

  private FloorArchetypes() {
  }

  public static FloorArchetype forDepth(int depth) {
    return depth % 2 == 0 ? CAVES : CATACOMBS;
  }

  public static FloorArchetype byId(String id) {
    return ALL.stream()
        .filter(archetype -> archetype.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown floor archetype: " + id));
  }
}
