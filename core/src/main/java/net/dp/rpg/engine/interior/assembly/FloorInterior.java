package net.dp.rpg.engine.interior.assembly;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomGeometry;

/**
 * A whole floor with the interior of every room generated and stitched into one
 * {@link TileMapData}. Immutable: {@link #withRoom} returns a new floor with one room replaced (a
 * reroll), re-stitching only the tile maps, never the layout.
 *
 * <p>Each room's map is placed at its origin cell and blitted skipping empty tiles, so a room with
 * a VOID hole (a ring) leaves the hole free for the room standing inside it. Stitching refuses to
 * overwrite a tile another room already filled and throws instead: two rooms never share a cell,
 * so an overlap means a blueprint or an offset is wrong, and silently letting one room paint over
 * another would hide exactly that.
 *
 * <p>Walls between neighbouring rooms are two tiles thick because every cell paints its own band,
 * and a door tile exists on both sides of that double wall; both belong to the same link in the
 * floor graph, which is what keeps two rooms generated independently consistent at their border.
 */
public final class FloorInterior {

  private final FloorLayout layout;

  private final List<GeneratedRoom> rooms;

  private final int minCellX;

  private final int minCellY;

  private final int cellsAcross;

  private final int cellsDown;

  private final TileMapData map;

  private FloorInterior(FloorLayout layout, List<GeneratedRoom> rooms, int minCellX, int minCellY,
      int cellsAcross, int cellsDown) {
    this.layout = layout;
    this.rooms = List.copyOf(rooms);
    this.minCellX = minCellX;
    this.minCellY = minCellY;
    this.cellsAcross = cellsAcross;
    this.cellsDown = cellsDown;
    this.map = stitch();
  }

  /** Generates every room of the layout, each from the blueprint the floor graph defines for it. */
  public static FloorInterior assemble(FloorLayout layout, InteriorGenerator generator) {
    List<RoomNode> nodes = layout.graph().rooms();
    List<GeneratedRoom> rooms = new ArrayList<>(nodes.size());

    for (RoomNode node : nodes) {
      if (node.index() != rooms.size()) {
        throw new IllegalStateException(
            "Room index %d is not its position %d in the graph".formatted(node.index(),
                rooms.size()));
      }

      rooms.add(generator.generate(layout.blueprintOf(node)));
    }

    int[] bounds = boundsOf(nodes);

    return new FloorInterior(layout, rooms, bounds[0], bounds[1], bounds[2], bounds[3]);
  }

  /** The same floor with one room's interior replaced, e.g. by a reroll. */
  public FloorInterior withRoom(int roomIndex, GeneratedRoom replacement) {
    List<GeneratedRoom> replaced = new ArrayList<>(rooms);

    replaced.set(roomIndex, replacement);

    return new FloorInterior(layout, replaced, minCellX, minCellY, cellsAcross, cellsDown);
  }

  public FloorLayout layout() {
    return layout;
  }

  public TileMapData map() {
    return map;
  }

  public List<GeneratedRoom> rooms() {
    return rooms;
  }

  public GeneratedRoom room(int roomIndex) {
    return rooms.get(roomIndex);
  }

  public int fallbackCount() {
    return (int) rooms.stream().filter(GeneratedRoom::isFallback).count();
  }

  public int retriedCount() {
    return (int) rooms.stream().filter(GeneratedRoom::wasRetried).count();
  }

  public int minCellX() {
    return minCellX;
  }

  public int minCellY() {
    return minCellY;
  }

  /** Tile x of the room's origin cell in the stitched floor map. */
  public int tileOffsetX(RoomNode room) {
    return RoomGeometry.originXOf(rooms.get(room.index()).blueprint().origin().x() - minCellX);
  }

  public int tileOffsetY(RoomNode room) {
    return RoomGeometry.originYOf(rooms.get(room.index()).blueprint().origin().y() - minCellY);
  }

  /** The room covering a tile of the stitched map, or null on empty ground or off the map. */
  public RoomNode roomAtTile(int tileX, int tileY) {
    if (tileX < 0 || tileY < 0 || tileX >= map.width() || tileY >= map.height()) {
      return null;
    }

    RoomCell cell = new RoomCell(minCellX + RoomGeometry.cellXOf(tileX),
        minCellY + RoomGeometry.cellYOf(tileY));

    return layout.graph().roomAt(cell);
  }

  private static int[] boundsOf(List<RoomNode> nodes) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (RoomNode node : nodes) {
      for (RoomCell cell : node.cells()) {
        minX = Math.min(minX, cell.x());
        minY = Math.min(minY, cell.y());
        maxX = Math.max(maxX, cell.x());
        maxY = Math.max(maxY, cell.y());
      }
    }

    return new int[] {minX, minY, maxX - minX + 1, maxY - minY + 1};
  }

  private TileMapData stitch() {
    int width = RoomGeometry.tileWidth(cellsAcross);
    int height = RoomGeometry.tileHeight(cellsDown);
    TileGrid ground = new TileGrid(width, height);
    TileGrid details = new TileGrid(width, height);
    List<TileMapObject> objects = new ArrayList<>();

    for (GeneratedRoom room : rooms) {
      int offsetX = RoomGeometry.originXOf(room.blueprint().origin().x() - minCellX);
      int offsetY = RoomGeometry.originYOf(room.blueprint().origin().y() - minCellY);
      TileGrid roomGround = room.map().requireLayer(TileLayerKind.GROUND).grid();

      requireNoOverlap(ground, roomGround, offsetX, offsetY, room);
      ground.blit(roomGround, offsetX, offsetY, true);
      details.blit(room.map().requireLayer(TileLayerKind.DETAILS).grid(), offsetX, offsetY, true);

      for (TileMapObject object : room.map().objects()) {
        objects.add(new TileMapObject(objects.size() + 1, object.name(), object.type(),
            object.x() + offsetX, object.y() + offsetY, object.width(), object.height(),
            object.properties()));
      }
    }

    List<TileLayer> layers = List.of(
        new TileLayer("Ground", TileLayerKind.GROUND, ground),
        new TileLayer("Details", TileLayerKind.DETAILS, details));
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put("floorArchetype", layout.archetypeId());
    properties.put("floorSeed", String.valueOf(layout.seed()));

    return new TileMapData(layers, objects, properties);
  }

  private static void requireNoOverlap(TileGrid floor, TileGrid room, int offsetX, int offsetY,
      GeneratedRoom owner) {
    for (int y = 0; y < room.getHeight(); y++) {
      for (int x = 0; x < room.getWidth(); x++) {
        if (room.get(x, y) != TileGrid.EMPTY && floor.get(offsetX + x, offsetY + y)
            != TileGrid.EMPTY) {
          throw new IllegalStateException(
              "Room at %s overwrites an already filled tile at floor tile %d,%d".formatted(
                  owner.blueprint().anchor(), offsetX + x, offsetY + y));
        }
      }
    }
  }
}
