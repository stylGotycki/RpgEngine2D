package net.dp.rpg.engine.adaptation;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.floor.FloorArchetype;

public final class AdaptiveSession {

  public static final double DEFAULT_BLEND = 0.5;

  // A game lasts this many floors; finishing the last one wins it.
  public static final int FLOORS = 4;

  private final FloorArchetype archetype;

  private final double blend;

  private final ProfileEstimator estimator = new ProfileEstimator();

  private final AdaptationPolicy policy = new AdaptationPolicy();

  private final List<FloorReport> reports = new ArrayList<>();

  private PlayerProfile profile = PlayerProfile.NEUTRAL;

  private Adaptation current;

  public AdaptiveSession(FloorArchetype archetype) {
    this(archetype, DEFAULT_BLEND);
  }

  public AdaptiveSession(FloorArchetype archetype, double blend) {
    if (blend < 0 || blend > 1) {
      throw new IllegalArgumentException("blend must be within 0..1, got " + blend);
    }

    this.archetype = archetype;
    this.blend = blend;
    this.current = policy.adapt(profile, archetype, 1);
  }

  public FloorArchetype archetype() {
    return archetype;
  }

  public PlayerProfile profile() {
    return profile;
  }

  public Adaptation current() {
    return current;
  }

  // Floors are numbered from 1, the one being played included.
  public int floor() {
    return reports.size() + 1;
  }

  public List<FloorReport> reports() {
    return List.copyOf(reports);
  }

  // True once the floor just finished was the last one of the game.
  public boolean finished() {
    return reports.size() >= FLOORS;
  }

  public PlayerProfile estimate(FloorMetrics metrics) {
    return estimator.estimate(metrics);
  }

  public FloorReport finishFloor(long seed, FloorMetrics metrics) {
    PlayerProfile measured = estimator.estimate(metrics);
    PlayerProfile before = profile;

    profile = profile.blend(measured, blend);
    // The report carries the settings of the floor that follows, so it is numbered one up.
    current = policy.adapt(profile, archetype, floor() + 1);

    FloorReport report = new FloorReport(floor(), seed, metrics, measured, before, profile,
        current);
    reports.add(report);
    return report;
  }
}
