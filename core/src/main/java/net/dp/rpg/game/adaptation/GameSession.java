package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.Gdx;
import net.dp.rpg.engine.adaptation.AdaptiveSession;
import net.dp.rpg.engine.adaptation.FloorReport;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;

// What outlives one floor: the adaptive profile, the session log and the floor being measured.
// Player state (stats, items) still travels in LevelBuilder; it joins here with the stairs.
public class GameSession {

  private final AdaptiveSession adaptive;

  private final SessionLog log = new SessionLog();

  private MetricsCollector collector;

  private long seed;

  public GameSession(FloorArchetype archetype) {
    this.adaptive = new AdaptiveSession(archetype);
  }

  public static GameSession start() {
    return new GameSession(FloorArchetypes.CATACOMBS);
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

  public void beginFloor(long seed, MetricsCollector collector) {
    if (this.collector != null) {
      this.collector.close();
    }

    this.seed = seed;
    this.collector = collector;
    collector.subscribe();
  }

  public FloorReport finishFloor() {
    if (collector == null) {
      throw new IllegalStateException("No floor is being played");
    }

    collector.close();
    FloorReport report = adaptive.finishFloor(seed, collector.snapshot());
    collector = null;
    log.write(report);

    if (Gdx.app != null) {
      Gdx.app.log("Session", "floor " + report.floor() + " -> " + report.after().values()
          + ", log " + log.path());
    }
    return report;
  }
}
