package net.dp.rpg.game;

import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.content.RoomContent;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public record TileRoom(int id, RoomNode node, GeneratedRoom generated, RoomContent content) {

  public RoomShape shape() {
    return generated.blueprint().shape();
  }

  public RoomCell origin() {
    return node.origin();
  }

  public TileMapData map() {
    return generated.map();
  }

  public Set<RoomEdge> doors() {
    return generated.blueprint().doors().keySet();
  }

  public Map<RoomEdge, DoorType> doorTypes() {
    return generated.blueprint().doors();
  }

  public DoorType doorType(RoomEdge localEdge) {
    return doorTypes().getOrDefault(localEdge, DoorType.NORMAL);
  }

  public String label() {
    return "room-%02d %s".formatted(id, node.type());
  }

  public boolean containsCell(RoomCell floorCell) {
    return shape().contains(new RoomCell(floorCell.x() - origin().x(), floorCell.y()
        - origin().y()));
  }

  public RoomCell toFloorCell(RoomCell localCell) {
    return localCell.translated(origin().x(), origin().y());
  }
}
