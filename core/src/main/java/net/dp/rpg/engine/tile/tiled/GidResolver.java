package net.dp.rpg.engine.tile.tiled;

import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

import java.util.Map;

public final class GidResolver {

  private static final int UNMAPPED = Integer.MIN_VALUE;

  private final int[] runtimeIdByGid;

  private GidResolver(int[] runtimeIdByGid) {
    this.runtimeIdByGid = runtimeIdByGid;
  }

  public static Builder builder(TileTypeRegistry registry) {
    return new Builder(registry);
  }

  public int resolve(int gid, String context) {
    if (gid == 0) {
      return TileGrid.EMPTY;
    }

    if (gid < 0 || gid >= runtimeIdByGid.length || runtimeIdByGid[gid] == UNMAPPED) {
      throw new InvalidTiledFormatException("%s uses global tile id %d, which no loaded tileset binds"
          .formatted(context, gid));
    }

    return runtimeIdByGid[gid];
  }

  public static final class Builder {

    private final TileTypeRegistry registry;

    private final java.util.Map<Integer, Integer> runtimeIdByGid = new java.util.TreeMap<>();

    private Builder(TileTypeRegistry registry) {
      this.registry = registry;
    }

    public Builder add(int firstGid, TilesetDefinition tileset) {
      Map<Integer, String> typeIdByLocalId = tileset.binding().typeIdByLocalId();

      typeIdByLocalId.forEach((localId, typeId) ->
          runtimeIdByGid.put(firstGid + localId, registry.requireRuntimeId(typeId)));

      return this;
    }

    public GidResolver build() {
      if (runtimeIdByGid.isEmpty()) {
        throw new InvalidTiledFormatException("Map references no usable tileset");
      }

      int maxGid = ((java.util.TreeMap<Integer, Integer>) runtimeIdByGid).lastKey();
      int[] lookup = new int[maxGid + 1];

      java.util.Arrays.fill(lookup, UNMAPPED);
      runtimeIdByGid.forEach((gid, runtimeId) -> lookup[gid] = runtimeId);

      return new GidResolver(lookup);
    }
  }
}
