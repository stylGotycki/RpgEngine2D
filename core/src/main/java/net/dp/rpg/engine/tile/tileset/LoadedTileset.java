package net.dp.rpg.engine.tile.tileset;

import net.dp.rpg.engine.tile.TileType;

import java.util.List;

public record LoadedTileset(TilesetDefinition definition, List<TileType> types) {

  public LoadedTileset {
    if (definition == null) {
      throw new IllegalArgumentException("Tileset definition must not be null");
    }

    types = types == null ? List.of() : List.copyOf(types);
  }
}
