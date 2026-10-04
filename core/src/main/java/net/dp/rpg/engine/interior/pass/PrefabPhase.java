package net.dp.rpg.engine.interior.pass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.prefab.Prefab;
import net.dp.rpg.engine.interior.prefab.PrefabLibrary;
import net.dp.rpg.engine.interior.prefab.PrefabPlacer;
import net.dp.rpg.engine.interior.prefab.PrefabRequirement;
import net.dp.rpg.engine.tile.TileMapObject;

public final class PrefabPhase {

  private final Map<RoomType, List<PrefabRequirement>> requirements;

  private final PrefabLibrary library;

  private final PrefabPlacer placer;

  public PrefabPhase(Map<RoomType, List<PrefabRequirement>> requirements, PrefabLibrary library,
                     PrefabPlacer placer) {
    if (requirements == null || library == null || placer == null) {
      throw new IllegalArgumentException("Requirements, library and placer must not be null");
    }

    this.requirements = requirements;
    this.library = library;
    this.placer = placer;
  }

  public List<TileMapObject> apply(RoomCanvas canvas, Random random) {
    List<PrefabRequirement> typeRequirements =
        requirements.getOrDefault(canvas.blueprint().type(), List.of());
    List<TileMapObject> accessPoints = new ArrayList<>();

    for (PrefabRequirement requirement : typeRequirements) {
      for (int i = 0; i < requirement.count(); i++) {
        placeOne(canvas, requirement, random, accessPoints);
      }
    }

    return accessPoints;
  }

  private void placeOne(RoomCanvas canvas, PrefabRequirement requirement, Random random,
                        List<TileMapObject> accessPoints) {
    if (!library.hasGroup(requirement.group())) {
      requireOptional(requirement, "motif has no prefab in group '%s'".formatted(requirement.group()));

      return;
    }

    Prefab prefab = library.draw(requirement.group(), random);
    PrefabPlacer.Placement placement = placer.place(canvas, prefab, random);

    if (placement == null) {
      requireOptional(requirement, "no position fit prefab group '%s' in this room"
          .formatted(requirement.group()));

      return;
    }

    accessPoints.addAll(placement.accessPoints());
  }

  private void requireOptional(PrefabRequirement requirement, String reason) {
    if (requirement.mandatory()) {
      throw new IllegalStateException("Mandatory prefab requirement unmet: %s".formatted(reason));
    }
  }
}
