package net.dp.rpg.engine.interior.corpus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.RoomShape;

public record CorpusSample(
    String source,
    TileMapData map,
    RoomShape shape,
    Set<RoomType> roomTypes,
    ZoneMap zones,
    List<PrefabRegion> prefabs,
    List<TileMapObject> markers) {

  public CorpusSample {
    roomTypes = Collections.unmodifiableSet(EnumSet.copyOf(roomTypes));
    prefabs = List.copyOf(prefabs);
    markers = List.copyOf(markers);
  }

  public int width() {
    return map.width();
  }

  public int height() {
    return map.height();
  }

  public boolean appliesTo(RoomType type) {
    return roomTypes.contains(type);
  }

  public TileLayer ground() {
    return map.requireLayer(TileLayerKind.GROUND);
  }

  public Optional<TileLayer> details() {
    return map.findLayer(TileLayerKind.DETAILS);
  }

  public Zone zoneAt(int x, int y) {
    return zones.zoneAt(x, y);
  }

  /** The fixed-element region covering the tile, or null. */
  public PrefabRegion prefabAt(int x, int y) {
    for (PrefabRegion prefab : prefabs) {
      if (prefab.contains(x, y)) {
        return prefab;
      }
    }

    return null;
  }

  public boolean isInPrefab(int x, int y) {
    return prefabAt(x, y) != null;
  }
}
