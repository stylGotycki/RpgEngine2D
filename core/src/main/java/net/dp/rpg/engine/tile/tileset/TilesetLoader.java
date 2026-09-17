package net.dp.rpg.engine.tile.tileset;

@FunctionalInterface
public interface TilesetLoader {

  LoadedTileset load(String internalPath);
}
