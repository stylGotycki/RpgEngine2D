package net.dp.rpg.demo.adaptation;

import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import net.dp.rpg.engine.adaptation.Adaptation;
import net.dp.rpg.engine.adaptation.FloorSample;
import net.dp.rpg.engine.adaptation.FloorStatistics;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.game.level.PlaceholderInterior;

// Generates whole floors (with WFC interiors) off the render thread. The interior data is
// loaded on the GL thread and then only touched here; a newer request cancels the one running.
final class SampleWorker {

  static final int FLOORS = 16;

  static final long FIRST_SEED = 40_000_000L;

  private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "adaptation-samples");
    thread.setDaemon(true);
    return thread;
  });

  private final AtomicInteger latest = new AtomicInteger();

  private final FloorArchetype archetype;

  private final TileSystem tiles;

  private final InteriorGenerator interiors;

  SampleWorker(FloorArchetype archetype) {
    this.archetype = archetype;
    this.tiles = new TileSystem(1L);
    tiles.loadTileset(PlaceholderInterior.TILESET);
    InteriorContext context = PlaceholderInterior.load(tiles);
    this.interiors = PlaceholderInterior.generator(context, tiles);
  }

  // Neutral may be null when its means are already known; the callback runs on the GL thread.
  void sample(Adaptation adaptation, Adaptation neutral,
      BiConsumer<FloorStatistics, FloorStatistics> done) {
    int request = latest.incrementAndGet();

    executor.execute(() -> {
      try {
        FloorStatistics neutralStats = neutral == null ? null : run(neutral, request);
        FloorStatistics adapted = run(adaptation, request);

        if (adapted != null && (neutral == null || neutralStats != null)) {
          Gdx.app.postRunnable(() -> done.accept(adapted, neutralStats));
        }
      } catch (RuntimeException e) {
        Gdx.app.error("Adaptation", "Sampling failed", e);
      }
    });
  }

  // Null when a newer request came in meanwhile.
  private FloorStatistics run(Adaptation adaptation, int request) {
    List<FloorSample> samples = new ArrayList<>();

    for (int i = 0; i < FLOORS; i++) {
      if (latest.get() != request) {
        return null;
      }
      samples.add(FloorSample.generate(archetype, adaptation, FIRST_SEED + i, interiors,
          tiles.types()));
    }

    return FloorStatistics.of(samples);
  }

  void shutdown() {
    executor.shutdownNow();
    tiles.dispose();
  }
}
