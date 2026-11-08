package net.dp.rpg.engine.floor;

public record FloorDescriptor(String archetypeId, long seed, int attempt, FloorSettings settings) {

  public static final int DIRECT = -1;

  public FloorDescriptor {
    if (archetypeId == null || archetypeId.isBlank()) {
      throw new IllegalArgumentException("Archetype id must not be blank");
    }

    if (attempt < DIRECT) {
      throw new IllegalArgumentException("attempt must be DIRECT or at least 0, got " + attempt);
    }

    if (settings == null) {
      throw new IllegalArgumentException("Settings must not be null");
    }
  }

  public long effectiveSeed() {
    return attempt == DIRECT ? seed : RandomSource.mix(seed + attempt);
  }
}
