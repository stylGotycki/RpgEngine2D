package net.dp.rpg.demo.floor;

import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.tile.TileMapData;

public record DemoFloor(FloorLayout layout, GridBounds bounds, TileMapData map, long seed) {

  public String label() {
    return "floor-%d".formatted(seed);
  }
}
