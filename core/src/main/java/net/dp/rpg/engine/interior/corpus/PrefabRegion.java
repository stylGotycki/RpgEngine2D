package net.dp.rpg.engine.interior.corpus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.tile.TileMapObject;

public record PrefabRegion(
    int x,
    int y,
    int width,
    int height,
    String group,
    Map<String, Object> properties,
    List<TileMapObject> objects) {

  public PrefabRegion {
    properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    objects = List.copyOf(objects);
  }

  public boolean contains(int tileX, int tileY) {
    return tileX >= x && tileX < x + width && tileY >= y && tileY < y + height;
  }

  public boolean overlaps(PrefabRegion other) {
    return x < other.x + other.width && other.x < x + width
        && y < other.y + other.height && other.y < y + height;
  }

  public List<TileMapObject> accessPoints() {
    return objects.stream().filter(object -> object.isType(CorpusScanner.ACCESS_TYPE)).toList();
  }
}
