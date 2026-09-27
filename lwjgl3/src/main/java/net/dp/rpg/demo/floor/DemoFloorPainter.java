package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.type.RoomType;
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

  private static final String WALKER_FLOOR = "floor.grass";

  private static final String WALKER_BADGE = "obstacle.rubble";

  private static final int WALKER_ARM = 4;

  private static final int BADGE_ARM = 2;

  private static final float WORN_WALL_CHANCE = 0.10f;

  private static final float TORCH_CHANCE = 0.06f;

  private static final Map<RoomType, Palette> PALETTES = palettes();

  private final TileTypeRegistry registry;

  public DemoFloorPainter(TileTypeRegistry registry) {
    this.registry = registry;
  }

  public TileMapData paint(FloorLayout floor, GridBounds bounds, long seed, FloorReveal reveal) {
    int width = RoomGeometry.tileWidth(bounds.width());
    int height = RoomGeometry.tileHeight(bounds.height());

    TileGrid ground = new TileGrid(width, height);
    TileGrid details = new TileGrid(width, height);
    Random random = new Random(seed);

    for (RoomNode room : floor.graph().rooms()) {
      if (reveal.shows(room)) {
        paintRoom(ground, details, room, reveal, random);
      }
    }

    for (RoomLink link : floor.graph().links()) {
      if (reveal.shows(link)) {
        punchDoor(ground, link);
      }
    }

    scatterTorches(ground, details, random);

    return new TileMapData(
        List.of(
            new TileLayer("Ground", TileLayerKind.GROUND, ground),
            new TileLayer("Details", TileLayerKind.DETAILS, details)),
        markers(floor, reveal),
        properties(floor, seed, reveal));
  }

  private void paintRoom(TileGrid ground, TileGrid details, RoomNode room, FloorReveal reveal, Random random) {
    Palette palette = PALETTES.getOrDefault(room.type(), PALETTES.get(RoomType.NORMAL));
    int wallId = registry.requireRuntimeId(WALL);
    int wornId = registry.requireRuntimeId(WALL_WORN);
    int floorId = registry.requireRuntimeId(palette.floor());

    for (RoomCell cell : room.cells()) {
      boolean here = reveal.isWalker(cell);
      int fill = here ? registry.requireRuntimeId(WALKER_FLOOR) : floorId;

      ground.fillRect(cell.tileOriginX(), cell.tileOriginY(),
          RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT, fill);

      if (here) {
        paintCross(details, cell, registry.requireRuntimeId(WALKER_BADGE), WALKER_ARM);
      } else if (palette.badge() != null) {
        paintCross(details, cell, registry.requireRuntimeId(palette.badge()), BADGE_ARM);
      }
    }

    for (RoomEdge edge : room.outerEdges()) {
      paintWall(ground, edge, wallId, wornId, random);
    }
  }

  private void paintCross(TileGrid details, RoomCell cell, int tileId, int arm) {
    int centreX = cell.centerTileX();
    int centreY = cell.centerTileY();

    for (int offset = -arm; offset <= arm; offset++) {
      details.set(centreX + offset, centreY, tileId);
      details.set(centreX, centreY + offset, tileId);
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

  private void scatterTorches(TileGrid ground, TileGrid details, Random random) {
    int torchId = registry.requireRuntimeId("decoration.torch");

    for (int y = 1; y < ground.getHeight() - 1; y++) {
      for (int x = 1; x < ground.getWidth() - 1; x++) {
        boolean free = details.get(x, y) == TileGrid.EMPTY;

        if (free && isWalkableGround(ground, x, y) && touchesWall(ground, x, y)
            && random.nextFloat() < TORCH_CHANCE) {
          details.set(x, y, torchId);
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

  private List<TileMapObject> markers(FloorLayout floor, FloorReveal reveal) {
    List<TileMapObject> markers = new ArrayList<>();
    int id = 1;

    for (RoomNode room : floor.graph().rooms()) {
      if (!reveal.shows(room)) {
        continue;
      }

      RoomCell cell = room.anchor();
      String type = room.type().name();

      markers.add(new TileMapObject(id++, type.toLowerCase(Locale.ROOT) + "-" + room.index(), type,
          cell.centerTileX() + 0.5f, cell.centerTileY() + 0.5f, 0f, 0f,
          Map.of("room", room.index(), "depth", room.depth(), "phase", room.phase().name())));
    }

    return markers;
  }

  private Map<String, Object> properties(FloorLayout floor, long seed, FloorReveal reveal) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put("seed", seed);
    properties.put("rooms", floor.rooms());
    properties.put("revealed", reveal.rooms().size());
    properties.put("criticalLength", floor.criticalLength());
    properties.put("holeRooms", floor.holeRooms());
    properties.put("extraDoors", floor.extraDoors());

    return properties;
  }

  private static Map<RoomType, Palette> palettes() {
    Map<RoomType, Palette> palettes = new EnumMap<>(RoomType.class);

    palettes.put(RoomType.START, new Palette("floor.grass", "decoration.flowers"));
    palettes.put(RoomType.NORMAL, new Palette("floor.stone", null));
    palettes.put(RoomType.PUZZLE, new Palette("floor.stone_cracked", "decoration.flowers"));
    palettes.put(RoomType.EMPTY, new Palette("floor.dirt", null));
    palettes.put(RoomType.COLLECTIBLE, new Palette("floor.stone", "decoration.bones"));
    palettes.put(RoomType.POWER_FIELD, new Palette("floor.grass", "decoration.torch"));
    palettes.put(RoomType.SHOP, new Palette("floor.dirt", "decoration.torch"));
    palettes.put(RoomType.MINIBOSS, new Palette("floor.sand", "decoration.bones"));
    palettes.put(RoomType.BOSS, new Palette("floor.sand", "decoration.torch"));
    palettes.put(RoomType.VAULT, new Palette("floor.stone_cracked", "decoration.bones"));

    return palettes;
  }

  private record Palette(String floor, String badge) {
  }
}
