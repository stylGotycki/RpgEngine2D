package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class DemoRoomPainter {

  private static final String FLOOR = "floor.stone";

  private static final String FLOOR_WORN = "floor.stone_cracked";

  private static final String WALL = "wall.stone";

  private static final String DOOR = "door.wood";

  private final TileTypeRegistry registry;

  private final int floorId;

  private final int wornFloorId;

  private final int wallId;

  private final int doorId;

  private final List<Integer> decorationIds;

  public DemoRoomPainter(TileTypeRegistry registry) {
    this.registry = registry;
    this.floorId = registry.requireRuntimeId(FLOOR);
    this.wornFloorId = registry.requireRuntimeId(FLOOR_WORN);
    this.wallId = registry.requireRuntimeId(WALL);
    this.doorId = registry.requireRuntimeId(DOOR);
    this.decorationIds = runtimeIdsWithTag("decoration");
  }

  public DemoRoom paint(DemoFloorGenerator.RoomDraft draft, long seed) {
    RoomShape shape = draft.getShape();
    Random random = new Random(seed * 31 + draft.getId());

    TileGrid ground = new TileGrid(shape.tileWidth(), shape.tileHeight());
    TileGrid details = new TileGrid(shape.tileWidth(), shape.tileHeight());

    for (RoomCell cell : shape.cells()) {
      paintCell(ground, shape, cell);
    }

    draft.getDoors().forEach(door -> ground.set(door.doorTileX(), door.doorTileY(), doorId));

    scatter(ground, details, random);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put("roomId", draft.getId());
    properties.put("cells", shape.size());
    properties.put(RoomShapes.SHAPE_PROPERTY, RoomShapes.encode(shape));

    TileMapData map = new TileMapData(
        List.of(
            new TileLayer("Ground", TileLayerKind.GROUND, ground),
            new TileLayer("Details", TileLayerKind.DETAILS, details)),
        markers(draft, shape),
        properties);

    return new DemoRoom(draft.getId(), shape, draft.getOrigin(), draft.getDoors(), map);
  }

  private void paintCell(TileGrid ground, RoomShape shape, RoomCell cell) {
    int originX = cell.tileOriginX();
    int originY = cell.tileOriginY();

    ground.fillRect(originX, originY, RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT, floorId);

    for (Direction direction : Direction.values()) {
      if (shape.contains(cell.neighbour(direction))) {
        continue;
      }

      paintWall(ground, originX, originY, direction);
    }
  }

  private void paintWall(TileGrid ground, int originX, int originY, Direction direction) {
    int lastX = originX + RoomGeometry.CELL_WIDTH - 1;
    int lastY = originY + RoomGeometry.CELL_HEIGHT - 1;

    switch (direction) {
      case NORTH -> ground.fillRect(originX, originY, RoomGeometry.CELL_WIDTH, 1, wallId);
      case SOUTH -> ground.fillRect(originX, lastY, RoomGeometry.CELL_WIDTH, 1, wallId);
      case WEST -> ground.fillRect(originX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId);
      case EAST -> ground.fillRect(lastX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId);
    }
  }

  private void scatter(TileGrid ground, TileGrid details, Random random) {
    for (int y = 0; y < ground.getHeight(); y++) {
      for (int x = 0; x < ground.getWidth(); x++) {
        if (ground.get(x, y) != floorId) {
          continue;
        }

        if (random.nextFloat() < 0.08f) {
          ground.set(x, y, wornFloorId);
        } else if (!decorationIds.isEmpty() && random.nextFloat() < 0.03f) {
          details.set(x, y, decorationIds.get(random.nextInt(decorationIds.size())));
        }
      }
    }
  }

  private List<TileMapObject> markers(DemoFloorGenerator.RoomDraft draft, RoomShape shape) {
    RoomCell firstCell = shape.cells().iterator().next();

    float centerX = firstCell.tileOriginX() + RoomGeometry.CENTER_X + 0.5f;
    float centerY = firstCell.tileOriginY() + RoomGeometry.CENTER_Y + 0.5f;

    String type = draft.getId() == 0 ? "PLAYER_START" : "ENEMY_SPAWN";

    return List.of(new TileMapObject(1, type.toLowerCase(java.util.Locale.ROOT), type,
        centerX, centerY, 0f, 0f, Map.of()));
  }

  private List<Integer> runtimeIdsWithTag(String tag) {
    List<Integer> ids = new ArrayList<>();

    for (TileType type : registry.findByTag(tag)) {
      ids.add(registry.requireRuntimeId(type.id()));
    }

    return ids;
  }
}
