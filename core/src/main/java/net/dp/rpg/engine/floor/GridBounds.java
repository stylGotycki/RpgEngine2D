package net.dp.rpg.engine.floor;

import net.dp.rpg.engine.tile.room.RoomCell;

/** The coarse grid the walker may not leave. */
public record GridBounds(int width, int height) {

  public GridBounds {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Grid must be at least 1x1, got %dx%d".formatted(width, height));
    }
  }

  public boolean contains(RoomCell cell) {
    return cell.x() >= 0 && cell.x() < width && cell.y() >= 0 && cell.y() < height;
  }

  public RoomCell center() {
    return new RoomCell(width / 2, height / 2);
  }
}
