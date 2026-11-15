package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.Gdx;
import net.dp.rpg.engine.adaptation.AdaptiveSession;
import net.dp.rpg.engine.adaptation.FloorMetrics;
import net.dp.rpg.engine.adaptation.FloorReport;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.RandomSource;

// What outlives one floor: the adaptive profile, the session log and the floor being measured.
// One session is one game: dying ends it, and the next game starts a new session with the
// neutral profile. Player state (stats, items, health) travels in PlayerCarry.
public class GameSession {

  private final AdaptiveSession adaptive;

  private final SessionLog log = new SessionLog();

  private final long runSeed;

  private MetricsCollector collector;

  private long floorSeed;

  public GameSession(FloorArchetype archetype, long runSeed) {
    this.adaptive = new AdaptiveSession(archetype);
    this.runSeed = runSeed;
  }

  public static GameSession start(long runSeed) {
    return new GameSession(FloorArchetypes.CATACOMBS, runSeed);
  }

  public AdaptiveSession adaptive() {
    return adaptive;
  }

  public MetricsCollector collector() {
    return collector;
  }

  public String logPath() {
    return log.path();
  }

  public long runSeed() {
    return runSeed;
  }

  // The first floor uses the run seed itself, so -Drpg.seed replays it; the others derive theirs.
  public long seedOf(int floor) {
    return floor == 1 ? runSeed : RandomSource.deriveSeed(runSeed, "floor:" + floor);
  }

  public long nextSeed() {
    return seedOf(adaptive.floor());
  }

  public void beginFloor(long seed, MetricsCollector collector) {
    if (this.collector != null) {
      this.collector.close();
    }

    this.floorSeed = seed;
    this.collector = collector;
    collector.subscribe();
  }

  public FloorReport finishFloor() {
    if (collector == null) {
      throw new IllegalStateException("No floor is being played");
    }

    collector.close();
    FloorReport report = adaptive.finishFloor(floorSeed, collector.snapshot());
    collector = null;
    log.write(report);

    if (Gdx.app != null) {
      Gdx.app.log("Session", "floor " + report.floor() + " -> " + report.after().values()
          + ", log " + log.path());
    }
    return report;
  }

  // The player died: the floor is logged as abandoned and stops listening. The profile is not
  // updated, because a new game starts from a new session anyway. Safe to call twice.
  public void abandonFloor() {
    if (collector == null) {
      return;
    }

    collector.close();
    FloorMetrics metrics = collector.snapshot();
    collector = null;
    log.writeAbandoned(adaptive.floor(), floorSeed, metrics);

    if (Gdx.app != null) {
      Gdx.app.log("Session", "floor " + adaptive.floor() + " abandoned, log " + log.path());
    }
  }
}
