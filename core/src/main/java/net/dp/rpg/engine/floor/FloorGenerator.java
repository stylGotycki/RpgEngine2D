package net.dp.rpg.engine.floor;

import java.util.List;

public final class FloorGenerator {

  public static final int MAX_ATTEMPTS = 40;

  private final FloorBuilder builder = new FloorBuilder();

  public FloorLayout generate(FloorArchetype archetype, long seed) {
    return generate(archetype, FloorSettings.of(archetype), seed);
  }

  public FloorLayout generate(FloorArchetype archetype, FloorSettings settings, long seed) {
    FloorArchetype tuned = archetype.with(settings);
    List<String> lastViolations = List.of();

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      FloorLayout floor = build(tuned, RandomSource.mix(seed + attempt));

      lastViolations = tuned.postconditions().violations(floor);

      if (lastViolations.isEmpty()) {
        return floor.withDescriptor(new FloorDescriptor(archetype.id(), seed, attempt, settings));
      }
    }

    throw new IllegalStateException("Floor %s failed %d attempts from seed %d, last: %s"
        .formatted(archetype.id(), MAX_ATTEMPTS, seed, lastViolations));
  }

  public FloorLayout buildOnce(FloorArchetype archetype, long seed) {
    return build(archetype, seed).withDescriptor(new FloorDescriptor(archetype.id(), seed,
        FloorDescriptor.DIRECT, FloorSettings.of(archetype)));
  }

  public FloorLayout replay(FloorArchetype archetype, FloorDescriptor descriptor) {
    if (!archetype.id().equals(descriptor.archetypeId())) {
      throw new IllegalArgumentException("Descriptor is for archetype '%s', not '%s'"
          .formatted(descriptor.archetypeId(), archetype.id()));
    }

    return build(archetype.with(descriptor.settings()), descriptor.effectiveSeed())
        .withDescriptor(descriptor);
  }

  private FloorLayout build(FloorArchetype archetype, long seed) {
    int roomCount = archetype.rollRoomCount(RandomSource.derive(seed, "size"));

    return builder.build(archetype.planFor(roomCount), archetype.boundsFor(roomCount),
        archetype.walker(), seed);
  }

  public FloorLayout generateForDepth(int depth, long runSeed) {
    FloorArchetype archetype = FloorArchetypes.forDepth(depth);

    return generate(archetype, RandomSource.mix(runSeed + depth));
  }
}
