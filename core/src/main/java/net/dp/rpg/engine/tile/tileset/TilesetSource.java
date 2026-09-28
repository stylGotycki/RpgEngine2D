package net.dp.rpg.engine.tile.tileset;

@FunctionalInterface
public interface TilesetSource {

  LoadedTileset load(String internalPath);
}
