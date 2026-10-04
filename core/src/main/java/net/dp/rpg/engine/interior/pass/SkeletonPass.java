package net.dp.rpg.engine.interior.pass;

import java.util.List;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.interior.model.DoorPalette;
import net.dp.rpg.engine.interior.model.InteriorModel;
import net.dp.rpg.engine.interior.model.WalkableClasses;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.wfc.WfcResult;
import net.dp.rpg.engine.wfc.WfcSolver;

public final class SkeletonPass {

  private final InteriorModel model;

  private final DoorPalette doors;

  private final PrefabPhase prefabs;

  private final WalkableSpinePass spine;

  private final WfcSolver solver;

  public SkeletonPass(InteriorModel model, DoorPalette doors, WalkableClasses walkable,
                      PrefabPhase prefabs, int maxAttempts) {
    if (model == null || doors == null || walkable == null || prefabs == null) {
      throw new IllegalArgumentException(
          "Model, door palette, walkable classes and prefab phase must not be null");
    }

    this.model = model;
    this.doors = doors;
    this.prefabs = prefabs;
    this.spine = new WalkableSpinePass(walkable, WalkableSpinePass.DEFAULT_WANDER_WEIGHT);
    this.solver = new WfcSolver(maxAttempts);
  }

  public Result generate(RoomBlueprint blueprint) {
    RoomCanvas canvas = RoomCanvas.of(blueprint, model, doors);
    List<TileMapObject> accessPoints =
        prefabs.apply(canvas, RandomSource.derive(blueprint.seed(), "interior.prefab"));
    boolean spineOk = spine.apply(canvas, accessPoints,
        RandomSource.derive(blueprint.seed(), "interior.spine"));

    if (!spineOk) {
      return new Result(canvas, null, null);
    }

    WfcResult ground = solver.solve(canvas.ground(),
        attempt -> RandomSource.derive(blueprint.seed(), "interior.ground#" + attempt));

    if (!ground.isSolved()) {
      return new Result(canvas, ground, null);
    }

    TileMapData map =
        canvas.toMapData(ground.state(), RandomSource.derive(blueprint.seed(), "interior.skin"));

    return new Result(canvas, ground, map);
  }

  public record Result(RoomCanvas canvas, WfcResult ground, TileMapData map) {

    public boolean isSolved() {
      return map != null;
    }
  }
}
