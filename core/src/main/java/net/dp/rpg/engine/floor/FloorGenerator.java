package net.dp.rpg.engine.floor;

import java.util.List;

public final class FloorGenerator {

  public static final int MAX_ATTEMPTS = 40;

  private final FloorBuilder builder = new FloorBuilder();

  public FloorLayout generate(FloorArchetype archetype, long seed) {
    List<String> lastViolations = List.of();

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      long effectiveSeed = RandomSource.mix(seed + attempt);
      FloorLayout floor = buildOnce(archetype, effectiveSeed);

      lastViolations = archetype.postconditions().violations(floor);

      if (lastViolations.isEmpty()) {
        return floor;
      }
    }

    throw new IllegalStateException("Floor %s failed %d attempts from seed %d, last: %s"
        .formatted(archetype.id(), MAX_ATTEMPTS, seed, lastViolations));
  }

  public FloorLayout buildOnce(FloorArchetype archetype, long seed) {
    int roomCount = archetype.rollRoomCount(RandomSource.derive(seed, "size"));

    return builder.build(archetype.planFor(roomCount), archetype.boundsFor(roomCount),
        archetype.walker(), seed);
  }

  public FloorLayout generateForDepth(int depth, long runSeed) {
    FloorArchetype archetype = FloorArchetypes.forDepth(depth);

    return generate(archetype, RandomSource.mix(runSeed + depth));
  }
}