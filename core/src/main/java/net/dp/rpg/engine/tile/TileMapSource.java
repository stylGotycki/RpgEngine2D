package net.dp.rpg.engine.tile;

@FunctionalInterface
public interface TileMapSource {

  TileMapData load(String internalPath);
}
