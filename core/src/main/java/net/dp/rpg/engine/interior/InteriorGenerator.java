package net.dp.rpg.engine.interior;

import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.detail.LearnedDetailPass;
import net.dp.rpg.engine.interior.pass.PrefabPhase;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.interior.pass.SkeletonPass;
import net.dp.rpg.engine.interior.prefab.PrefabPlacer;
import net.dp.rpg.engine.interior.prefab.PrefabRequirement;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.wfc.WfcState;

/**
 * The single entry point for turning a {@link RoomBlueprint} into a finished room: prefabs,
 * walkable spine, WFC and the learned details, wired once from an {@link InteriorContext}. Later
 * layers (enemies, collectibles, quests) hang off the {@link GeneratedRoom} this returns instead
 * of rebuilding the pipeline.
 *
 * <p>A room never fails the floor. An attempt that cannot be solved, finds no route between two
 * doors or cannot place a mandatory fixed element is retried with a seed derived from the
 * blueprint's own ({@code interior.retry#n}), so the retry sequence is as reproducible as the room;
 * after {@link Settings#roomAttempts()} failures the room becomes an {@link EmergencyRoom} and the
 * reason is kept in {@link GeneratedRoom#note()}. This is the "local degradation before anything
 * escalates" rule the floor generator already follows.
 */
public final class InteriorGenerator {

  /** Limits of one room: WFC restarts per attempt, and whole-room attempts before falling back. */
  public record Settings(int wfcAttempts, int roomAttempts) {

    public static final Settings DEFAULTS = new Settings(30, 4);

    public Settings {
      if (wfcAttempts <= 0 || roomAttempts <= 0) {
        throw new IllegalArgumentException("Attempt limits must be greater than zero");
      }
    }
  }

  private final InteriorContext context;

  private final SkeletonPass skeleton;

  private final LearnedDetailPass details;

  private final Settings settings;

  public InteriorGenerator(InteriorContext context, TileTypeRegistry types,
      Map<RoomType, List<PrefabRequirement>> requirements, Settings settings) {
    if (context == null || types == null || requirements == null || settings == null) {
      throw new IllegalArgumentException("Context, types, requirements and settings are required");
    }

    PrefabPhase prefabs =
        new PrefabPhase(requirements, context.prefabs(), new PrefabPlacer(context.classes()));

    this.context = context;
    this.settings = settings;
    this.skeleton = new SkeletonPass(context.model(), context.doors(), context.walkable(),
        prefabs, settings.wfcAttempts());
    this.details = new LearnedDetailPass(context.details(),
        runtimeId -> !types.require(runtimeId).walkable());
  }

  public InteriorContext context() {
    return context;
  }

  public GeneratedRoom generate(RoomBlueprint blueprint) {
    String lastFailure = "no attempt made";

    for (int attempt = 0; attempt < settings.roomAttempts(); attempt++) {
      RoomBlueprint used = attempt == 0 ? blueprint : blueprint.withSeed(
          RandomSource.deriveSeed(blueprint.seed(), "interior.retry#" + attempt));

      try {
        SkeletonPass.Result result = skeleton.generate(used);

        if (result.isSolved()) {
          return finish(used, result, attempt + 1);
        }

        lastFailure = describe(result);
      } catch (IllegalStateException exception) {
        lastFailure = exception.getMessage();
      }
    }

    return emergency(blueprint, lastFailure);
  }

  /** Details go on after WFC, so the map is built here, once, rather than taken from the pass. */
  private GeneratedRoom finish(RoomBlueprint used, SkeletonPass.Result result, int attempts) {
    RoomCanvas canvas = result.canvas();
    WfcState ground = result.ground().state();

    details.apply(canvas, ground, RandomSource.derive(used.seed(), "interior.details"));

    TileMapData map = canvas.toMapData(ground, RandomSource.derive(used.seed(), "interior.skin"));

    return new GeneratedRoom(used, canvas, ground, map, GeneratedRoom.Status.GENERATED, attempts,
        "");
  }

  private GeneratedRoom emergency(RoomBlueprint blueprint, String reason) {
    RoomCanvas canvas = RoomCanvas.of(blueprint, context.model(), context.doors());
    TileMapData map = EmergencyRoom.build(canvas, context,
        RandomSource.derive(blueprint.seed(), "interior.skin"));

    String note = "after %d attempts: %s".formatted(settings.roomAttempts(), reason);

    return new GeneratedRoom(blueprint, canvas, null, map, GeneratedRoom.Status.FALLBACK,
        settings.roomAttempts(), note);
  }

  private static String describe(SkeletonPass.Result result) {
    if (result.ground() == null) {
      return "the walkable spine found no route between two doors";
    }

    return "WFC did not solve (%s)".formatted(result.ground().status());
  }
}
