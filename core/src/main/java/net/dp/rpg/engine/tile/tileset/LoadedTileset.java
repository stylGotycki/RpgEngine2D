package net.dp.rpg.engine.tile.tileset;

import java.util.List;
import net.dp.rpg.engine.tile.TileType;

public record LoadedTileset(TilesetDefinition definition, List<TileType> types) {

  public LoadedTileset {
    if (definition == null) {
      throw new IllegalArgumentException("Tileset definition must not be null");
    }

    types = types == null ? List.of() : List.copyOf(types);
  }
}
