package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;

public final class DemoFloorPainter {

  private static final String WALL = "wall.stone";

  private static final String WALL_WORN = "wall.mossy";

  private static final String DOOR = "door.wood";

  private static final String DOOR_LOCKED = "door.locked";

  private static final String WALKER = "marker.walker";

  private static final String QUEST = "marker.quest";

  private static final String FALLBACK_FLOOR = "floor.normal";

  private static final int WALKER_ARM = 4;

  private static final int QUEST_INSET = 3;

  private static final Map<RoomType, String> FLOORS = floors();

  private final TileTypeRegistry registry;

  public DemoFloorPainter(TileTypeRegistry registry) {
    this.registry = registry;
  }

  public TileMapData paint(FloorLayout floor, GridBounds bounds, FloorReveal reveal) {
    int width = RoomGeometry.tileWidth(bounds.width());
    int height = RoomGeometry.tileHeight(bounds.height());

    TileGrid ground = new TileGrid(width, height);
    TileGrid details = new TileGrid(width, height);

    for (RoomNode room : floor.graph().rooms()) {
      if (reveal.shows(room)) {
        paintRoom(ground, details, room, reveal);
      }
    }

    for (RoomLink link : floor.graph().links()) {
      if (reveal.shows(link)) {
        punchDoor(ground, link);
      }
    }

    return new TileMapData(
        List.of(
            new TileLayer("Ground", TileLayerKind.GROUND, ground),
            new TileLayer("Details", TileLayerKind.DETAILS, details)),
        markers(floor, reveal),
        properties(floor, reveal));
  }

  private void paintRoom(TileGrid ground, TileGrid details, RoomNode room, FloorReveal reveal) {
    int floorId = registry.requireRuntimeId(FLOORS.getOrDefault(room.type(), FALLBACK_FLOOR));
    int wallId = registry.requireRuntimeId(WALL);
    int wornId = registry.requireRuntimeId(WALL_WORN);

    for (RoomCell cell : room.cells()) {
      ground.fillRect(cell.tileOriginX(), cell.tileOriginY(),
          RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT, floorId);

      if (room.questId() != null) {
        paintQuestCorners(details, cell);
      }

      if (reveal.isWalker(cell)) {
        paintCross(details, cell, registry.requireRuntimeId(WALKER), WALKER_ARM);
      }
    }

    for (RoomEdge edge : room.outerEdges()) {
      paintWall(ground, edge, wallId, wornId);
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

  private void paintQuestCorners(TileGrid details, RoomCell cell) {
    int questId = registry.requireRuntimeId(QUEST);
    int left = cell.tileOriginX() + QUEST_INSET;
    int top = cell.tileOriginY() + QUEST_INSET;
    int right = cell.tileOriginX() + RoomGeometry.CELL_WIDTH - 1 - QUEST_INSET;
    int bottom = cell.tileOriginY() + RoomGeometry.CELL_HEIGHT - 1 - QUEST_INSET;

    details.set(left, top, questId);
    details.set(right, top, questId);
    details.set(left, bottom, questId);
    details.set(right, bottom, questId);
  }

  private void paintWall(TileGrid ground, RoomEdge edge, int wallId, int wornId) {
    RoomCell cell = edge.cell();
    int originX = cell.tileOriginX();
    int originY = cell.tileOriginY();
    int lastX = originX + RoomGeometry.CELL_WIDTH - 1;
    int lastY = originY + RoomGeometry.CELL_HEIGHT - 1;

    switch (edge.direction()) {
      case NORTH -> paintRun(ground, originX, originY, RoomGeometry.CELL_WIDTH, 1, wallId, wornId);
      case SOUTH -> paintRun(ground, originX, lastY, RoomGeometry.CELL_WIDTH, 1, wallId, wornId);
      case WEST -> paintRun(ground, originX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId, wornId);
      case EAST -> paintRun(ground, lastX, originY, 1, RoomGeometry.CELL_HEIGHT, wallId, wornId);
    }
  }

  private void paintRun(TileGrid ground, int x, int y, int width, int height, int wallId, int wornId) {
    for (int offsetY = 0; offsetY < height; offsetY++) {
      for (int offsetX = 0; offsetX < width; offsetX++) {
        int tileX = x + offsetX;
        int tileY = y + offsetY;

        ground.set(tileX, tileY, (tileX * 7 + tileY * 13) % 11 == 0 ? wornId : wallId);
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

  private List<TileMapObject> markers(FloorLayout floor, FloorReveal reveal) {
    List<TileMapObject> markers = new ArrayList<>();
    int id = 1;

    for (RoomNode room : floor.graph().rooms()) {
      if (!reveal.shows(room)) {
        continue;
      }

      RoomCell cell = room.anchor();
      String type = room.type().name();
      Map<String, Object> properties = new LinkedHashMap<>();

      properties.put("room", room.index());
      properties.put("depth", room.depth());
      properties.put("phase", room.phase().name());

      if (room.questId() != null) {
        properties.put("quest", room.questId());
      }

      markers.add(new TileMapObject(id++, type.toLowerCase(Locale.ROOT) + "-" + room.index(), type,
          cell.centerTileX() + 0.5f, cell.centerTileY() + 0.5f, 0f, 0f, properties));
    }

    return markers;
  }

  private Map<String, Object> properties(FloorLayout floor, FloorReveal reveal) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put("archetype", floor.archetypeId());
    properties.put("seed", floor.seed());
    properties.put("rooms", floor.rooms());
    properties.put("revealed", reveal.rooms().size());
    properties.put("criticalLength", floor.criticalLength());
    properties.put("holeRooms", floor.holeRooms());
    properties.put("quests", floor.quests().size());

    return properties;
  }

  private static Map<RoomType, String> floors() {
    Map<RoomType, String> floors = new EnumMap<>(RoomType.class);

    floors.put(RoomType.START, "floor.start");
    floors.put(RoomType.NORMAL, "floor.normal");
    floors.put(RoomType.PUZZLE, "floor.puzzle");
    floors.put(RoomType.EMPTY, "floor.empty");
    floors.put(RoomType.COLLECTIBLE, "floor.collectible");
    floors.put(RoomType.POWER_FIELD, "floor.power");
    floors.put(RoomType.SHOP, "floor.shop");
    floors.put(RoomType.MINIBOSS, "floor.miniboss");
    floors.put(RoomType.BOSS, "floor.boss");
    floors.put(RoomType.VAULT, "floor.vault");

    return floors;
  }
}
