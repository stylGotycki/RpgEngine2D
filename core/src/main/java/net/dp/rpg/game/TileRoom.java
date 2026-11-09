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

/**
 * One room of the floor the player walks through: its node in the floor graph (type, depth,
 * quest), and its WFC interior. Coordinates follow the engine's y-up axis. Tile (x, y) of
 * {@link #map()} lies at world (origin.x * CELL_WIDTH + x, origin.y * CELL_HEIGHT + y), and
 * {@link #doors()} are edges of the room's own shape, so {@code doorTileX/Y} are tiles of
 * {@link #map()}. {@link #content()} spots use the same local tiles.
 */
public record TileRoom(int id, RoomNode node, GeneratedRoom generated, RoomContent content) {

  public RoomShape shape() {
    return generated.blueprint().shape();
  }

  /** The floor cell of the shape's cell (0, 0): where the room's map starts on the floor. */
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
