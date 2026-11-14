package net.dp.rpg.engine.interior;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.detail.LearnedDetailPass;
import net.dp.rpg.engine.interior.pass.PrefabPhase;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.interior.pass.RoomReachability;
import net.dp.rpg.engine.interior.pass.SkeletonPass;
import net.dp.rpg.engine.interior.prefab.PrefabPlacer;
import net.dp.rpg.engine.interior.prefab.PrefabRequirement;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.wfc.WfcState;

public final class InteriorGenerator {

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

  private final RoomReachability reachability;

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
    this.reachability = new RoomReachability(types);
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

        if (!result.isSolved()) {
          lastFailure = describe(result);
          continue;
        }

        GeneratedRoom room = finish(used, result, attempt + 1);
        Optional<String> problem = reachability.problem(room.canvas(), room.map());

        if (problem.isEmpty()) {
          return room;
        }

        lastFailure = problem.get();
      } catch (IllegalStateException exception) {
        lastFailure = exception.getMessage();
      }
    }

    return emergency(blueprint, lastFailure);
  }

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
      return "the walkable spine could not join the doors with walkable tiles";
    }

    return "WFC did not solve (%s)".formatted(result.ground().status());
  }
}
