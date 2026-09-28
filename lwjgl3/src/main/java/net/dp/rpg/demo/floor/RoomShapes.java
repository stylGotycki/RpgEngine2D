package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.exception.InvalidRoomShapeException;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class RoomShapes {

  public static final String SHAPE_PROPERTY = "roomCells";

  private static final String ROW_SEPARATOR = "|";

  private RoomShapes() {
  }

  public static String encode(RoomShape shape) {
    StringBuilder encoded = new StringBuilder();

    for (int y = 0; y < shape.cellsDown(); y++) {
      if (y > 0) {
        encoded.append(ROW_SEPARATOR);
      }

      for (int x = 0; x < shape.cellsAcross(); x++) {
        encoded.append(shape.contains(new RoomCell(x, y)) ? '#' : '.');
      }
    }

    return encoded.toString();
  }

  public static RoomShape of(TileMapData map) {
    Object encoded = map.properties().get(SHAPE_PROPERTY);

    if (encoded instanceof String text && !text.isBlank()) {
      return RoomShape.parse(text.split("\\" + ROW_SEPARATOR));
    }

    return derive(map);
  }

  private static RoomShape derive(TileMapData map) {
    int across = map.width() / RoomGeometry.CELL_WIDTH;
    int down = map.height() / RoomGeometry.CELL_HEIGHT;

    if (across < 1 || down < 1
        || map.width() % RoomGeometry.CELL_WIDTH != 0
        || map.height() % RoomGeometry.CELL_HEIGHT != 0) {
      throw new InvalidRoomShapeException("Map is %dx%d tiles, which is not a whole number of %dx%d cells"
          .formatted(map.width(), map.height(), RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT));
    }

    List<RoomCell> cells = new ArrayList<>();

    for (int cellY = 0; cellY < down; cellY++) {
      for (int cellX = 0; cellX < across; cellX++) {
        if (hasContent(map, cellX, cellY)) {
          cells.add(new RoomCell(cellX, cellY));
        }
      }
    }

    if (cells.isEmpty()) {
      throw new InvalidRoomShapeException("Map holds no tiles, so it has no room shape");
    }

    return RoomShape.of(cells);
  }

  private static boolean hasContent(TileMapData map, int cellX, int cellY) {
    int originX = RoomGeometry.originXOf(cellX);
    int originY = RoomGeometry.originYOf(cellY);

    for (int y = originY; y < originY + RoomGeometry.CELL_HEIGHT; y++) {
      for (int x = originX; x < originX + RoomGeometry.CELL_WIDTH; x++) {
        if (map.topTileAt(x, y) != TileGrid.EMPTY) {
          return true;
        }
      }
    }

    return false;
  }
}
