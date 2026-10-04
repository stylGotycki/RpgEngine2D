package net.dp.rpg.engine.interior.prefab;

import java.util.List;
import net.dp.rpg.engine.interior.corpus.CorpusScanner;
import net.dp.rpg.engine.interior.corpus.PrefabAnchor;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;

public record Prefab(
    String group,
    String source,
    int width,
    int height,
    TileGrid ground,
    TileGrid details,
    PrefabAnchor anchor,
    Direction drawnFacing,
    boolean flippable,
    double weight,
    List<TileMapObject> objects) {

  public Prefab {
    objects = List.copyOf(objects);

    if (anchor == PrefabAnchor.WALL && drawnFacing == null) {
      throw new IllegalArgumentException("WALL-anchored prefab '%s' from %s has no detected facing".formatted(group, source));
    }

    if (anchor != PrefabAnchor.WALL && drawnFacing != null) {
      throw new IllegalArgumentException("Prefab '%s' from %s has a facing but is not WALL-anchored".formatted(group, source));
    }
  }

  public List<TileMapObject> accessPoints() {
    return objects.stream().filter(object -> object.isType(CorpusScanner.ACCESS_TYPE)).toList();
  }
}
