package net.dp.rpg.engine.floor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class FloorContext {

  private final Map<String, Random> streams = new LinkedHashMap<>();

  private final FloorPlan plan;

  private final RoomTypes types;

  private final Loadout loadout;

  private final GridBounds bounds;

  private final WalkerSettings settings;

  private final long seed;

  private FloorContext(FloorPlan plan, RoomTypes types, Loadout loadout, GridBounds bounds,
                       WalkerSettings settings, long seed) {
    this.plan = plan;
    this.types = types;
    this.loadout = loadout;
    this.bounds = bounds;
    this.settings = settings;
    this.seed = seed;
  }

  public static FloorContext of(FloorPlan plan, GridBounds bounds, WalkerSettings settings, long seed) {
    RoomTypes types = RoomTypes.of(plan.roomTypes(), plan.defaultShapes());

    return new FloorContext(plan, types, Loadout.roll(types, RandomSource.derive(seed, "loadout")),
        bounds, settings, seed);
  }

  public Random stream(String label) {
    return streams.computeIfAbsent(label, key -> RandomSource.derive(seed, key));
  }

  public FloorPlan plan() {
    return plan;
  }

  public RoomTypes types() {
    return types;
  }

  public Loadout loadout() {
    return loadout;
  }

  public GridBounds bounds() {
    return bounds;
  }

  public WalkerSettings settings() {
    return settings;
  }

  public long seed() {
    return seed;
  }
}
