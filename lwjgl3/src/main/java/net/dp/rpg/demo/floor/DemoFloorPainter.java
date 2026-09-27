package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.DoorType;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.RoomLink;
import net.dp.rpg.engine.floor.RoomNode;
import net.dp.rpg.engine.floor.RoomPhase;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;

public final class DemoFloorPainter {

  private static final String WALL = "wall.stone";

  private static final String WALL_WORN = "wall.mossy";

  private static final String DOOR = "door.wood";

  private static final String DOOR_LOCKED = "obstacle.rubble";

  private static final String FLOOR_PATH = "floor.stone";

  private static final String FLOOR_SIDE = "floor.dirt";

  private static final String FLOOR_START = "floor.grass";

  private static final String FLOOR_BOSS = "floor.sand";

  private static final String FLOOR_HOLE = "floor.stone_cracked";

  private static final float WORN_WALL_CHANCE = 0.10f;

  private static final float DECORATION_CHANCE = 0.02f;

  private static final float TORCH_CHANCE = 0.06f;

  private final TileTypeRegistry registry;

  public DemoFloorPainter(TileTypeRegistry registry) {
    this.registry = registry;
  }

  public TileMapData paint(FloorLayout floor, GridBounds bounds, long seed) {
    int width = RoomGeometry.tileWidth(bounds.width());
    int height = RoomGeometry.tileHeight(bounds.height());

    TileGrid ground = new TileGrid(width, height);
    TileGrid details = new TileGrid(width, height);
    Random random = new Random(seed);

    for (RoomNode room : floor.graph().rooms()) {
      paintRoom(ground, room, floorIdOf(room, floor), random);
    }

    for (RoomLink link : floor.graph().links()) {
      punchDoor(ground, link);
    }

    scatter(ground, details, random);

    return new TileMapData(
        List.of(
            new TileLayer("Ground", TileLayerKind.GROUND, ground),
            new TileLayer("Details", TileLayerKind.DETAILS, details)),
        markers(floor),
        properties(floor, seed));
  }

  private void paintRoom(TileGrid ground, RoomNode room, int floorId, Random random) {
    int wallId = registry.requireRuntimeId(WALL);
    int wornId = registry.requireRuntimeId(WALL_WORN);

    for (RoomCell cell : room.cells()) {
      ground.fillRect(cell.tileOriginX(), cell.tileOriginY(),
          RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT, floorId);
    }

    for (RoomEdge edge : room.outerEdges()) {
      paintWall(ground, edge, wallId, wornId, random);
    }
  }

  private void paintWall(TileGrid ground, RoomEdge edge, int wallId, int wornId, Random random) {
    RoomCell cell = edge.cell();
    int originX = cell.tileOriginX();
    int originY = cell.tileOriginY();
    int lastX = originX + RoomGeometry.CELL_WIDTH - 1;
    int lastY = originY + RoomGeometry.CELL_HEIGHT - 1;

    switch (edge.direction()) {
      case NORTH -> paintRun(ground, originX, originY, RoomGeometry.CELL_WIDTH, 1, wallId, wornId, random);
      case SOUTH -> paintRun(ground, originX, lastY, RoomGeometry.CELL_WIDTH, 1, wallId, wornId, random);
      case WEST -> paintRun(ground, originX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId, wornId, random);
      case EAST -> paintRun(ground, lastX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId, wornId, random);
    }
  }

  private void paintRun(TileGrid ground, int x, int y, int width, int height, int wallId, int wornId,
                        Random random) {
    for (int offsetY = 0; offsetY < height; offsetY++) {
      for (int offsetX = 0; offsetX < width; offsetX++) {
        ground.set(x + offsetX, y + offsetY, random.nextFloat() < WORN_WALL_CHANCE ? wornId : wallId);
      }
    }
  }

  private void punchDoor(TileGrid ground, RoomLink link) {
    int doorId = registry.requireRuntimeId(link.doorType() == DoorType.LOCKED ? DOOR_LOCKED : DOOR);
    RoomEdge near = link.edge();
    RoomEdge far = new RoomEdge(near.cell().neighbour(near.direction()), near.direction().opposite());

    ground.set(near.doorTileX(), near.doorTileY(), doorId);
    ground.set(far.doorTileX(), far.doorTileY(), doorId);
  }

  private void scatter(TileGrid ground, TileGrid details, Random random) {
    List<Integer> decorations = runtimeIdsWithTag("decoration");

    if (decorations.isEmpty()) {
      return;
    }

    int torchId = registry.requireRuntimeId("decoration.torch");

    for (int y = 1; y < ground.getHeight() - 1; y++) {
      for (int x = 1; x < ground.getWidth() - 1; x++) {
        if (!isWalkableGround(ground, x, y)) {
          continue;
        }

        if (touchesWall(ground, x, y) && random.nextFloat() < TORCH_CHANCE) {
          details.set(x, y, torchId);
        } else if (random.nextFloat() < DECORATION_CHANCE) {
          details.set(x, y, decorations.get(random.nextInt(decorations.size())));
        }
      }
    }
  }

  private boolean isWalkableGround(TileGrid ground, int x, int y) {
    int id = ground.get(x, y);

    return id != TileGrid.EMPTY && registry.require(id).walkable();
  }

  private boolean touchesWall(TileGrid ground, int x, int y) {
    for (Direction direction : Direction.values()) {
      int neighbour = ground.getOrEmpty(x + direction.getDeltaX(), y + direction.getDeltaY());

      if (neighbour != TileGrid.EMPTY && !registry.require(neighbour).walkable()) {
        return true;
      }
    }

    return false;
  }

  private int floorIdOf(RoomNode room, FloorLayout floor) {
    if (room == floor.graph().start()) {
      return registry.requireRuntimeId(FLOOR_START);
    }

    if (room == floor.boss()) {
      return registry.requireRuntimeId(FLOOR_BOSS);
    }

    if (room.phase() == RoomPhase.HOLE) {
      return registry.requireRuntimeId(FLOOR_HOLE);
    }

    return registry.requireRuntimeId(floor.criticalPath().contains(room) ? FLOOR_PATH : FLOOR_SIDE);
  }

  private List<TileMapObject> markers(FloorLayout floor) {
    List<TileMapObject> markers = new ArrayList<>();
    int id = 1;

    for (RoomNode room : floor.graph().rooms()) {
      RoomCell cell = room.anchor();
      String type = markerTypeOf(room, floor);

      markers.add(new TileMapObject(id++, type.toLowerCase(Locale.ROOT) + "-" + room.index(), type,
          cell.centerTileX() + 0.5f, cell.centerTileY() + 0.5f, 0f, 0f,
          Map.of("room", room.index(), "depth", room.depth(), "phase", room.phase().name())));
    }

    return markers;
  }

  private String markerTypeOf(RoomNode room, FloorLayout floor) {
    if (room == floor.graph().start()) {
      return "PLAYER_START";
    }

    if (room == floor.boss()) {
      return "BOSS";
    }

    return room.phase() == RoomPhase.HOLE ? "VAULT" : "ENEMY_SPAWN";
  }

  private Map<String, Object> properties(FloorLayout floor, long seed) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put("seed", seed);
    properties.put("rooms", floor.rooms());
    properties.put("criticalLength", floor.criticalLength());
    properties.put("holeRooms", floor.holeRooms());
    properties.put("extraDoors", floor.extraDoors());

    return properties;
  }

  private List<Integer> runtimeIdsWithTag(String tag) {
    List<Integer> ids = new ArrayList<>();

    registry.findByTag(tag).forEach(type -> ids.add(registry.requireRuntimeId(type.id())));

    return ids;
  }
}
