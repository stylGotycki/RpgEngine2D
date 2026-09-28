package net.dp.rpg.demo.floor;

import java.util.Set;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public record DemoRoom(int id, RoomShape shape, RoomCell origin, Set<RoomEdge> doors, TileMapData map) {

  public String label() {
    return "room-%02d".formatted(id);
  }

  public RoomCell toFloorCell(RoomCell localCell) {
    return localCell.translated(origin.x(), origin.y());
  }
}
