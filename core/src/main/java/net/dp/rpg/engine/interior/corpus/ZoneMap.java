package net.dp.rpg.engine.interior.corpus;

import java.util.function.Predicate;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class ZoneMap {

  private final int width;

  private final int height;

  private final Zone[] zones;

  private ZoneMap(int width, int height, Zone[] zones) {
    this.width = width;
    this.height = height;
    this.zones = zones;
  }

  public static ZoneMap of(RoomShape shape, Predicate<RoomEdge> hasDoor) {
    int width = shape.tileWidth();
    int height = shape.tileHeight();
    Zone[] zones = new Zone[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        zones[x + y * width] = classify(shape, hasDoor, x, y);
      }
    }

    return new ZoneMap(width, height, zones);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public boolean isInside(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  public Zone zoneAt(int x, int y) {
    if (!isInside(x, y)) {
      throw new IllegalArgumentException("Tile %d,%d is outside the %dx%d zone map".formatted(x, y, width, height));
    }

    return zones[x + y * width];
  }

  public int count(Zone zone) {
    int count = 0;

    for (Zone candidate : zones) {
      if (candidate == zone) {
        count++;
      }
    }

    return count;
  }

  public String toDebugString() {
    StringBuilder builder = new StringBuilder((width + 1) * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        builder.append(switch (zones[x + y * width]) {
          case VOID -> ' ';
          case WALL -> '#';
          case DOOR -> 'D';
          case INTERIOR -> '.';
        });
      }

      builder.append(System.lineSeparator());
    }

    return builder.toString();
  }

  private static Zone classify(RoomShape shape, Predicate<RoomEdge> hasDoor, int x, int y) {
    RoomCell cell = new RoomCell(RoomGeometry.cellXOf(x), RoomGeometry.cellYOf(y));

    if (!shape.contains(cell)) {
      return Zone.VOID;
    }

    int localX = x - cell.tileOriginX();
    int localY = y - cell.tileOriginY();
    boolean onBand = false;

    for (Direction direction : Direction.values()) {
      if (shape.contains(cell.neighbour(direction)) || !onEdge(direction, localX, localY)) {
        continue;
      }

      onBand = true;

      boolean doorSlot = cell.doorTileX(direction) == x && cell.doorTileY(direction) == y;

      if (doorSlot && hasDoor.test(new RoomEdge(cell, direction))) {
        return Zone.DOOR;
      }
    }

    return onBand ? Zone.WALL : Zone.INTERIOR;
  }

  private static boolean onEdge(Direction direction, int localX, int localY) {
    return switch (direction) {
      case NORTH -> localY == 0;
      case SOUTH -> localY == RoomGeometry.CELL_HEIGHT - 1;
      case WEST -> localX == 0;
      case EAST -> localX == RoomGeometry.CELL_WIDTH - 1;
    };
  }
}
