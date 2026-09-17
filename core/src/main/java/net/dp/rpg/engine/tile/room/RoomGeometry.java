package net.dp.rpg.engine.tile.room;

public final class RoomGeometry {

  public static final int CELL_WIDTH = 25;

  public static final int CELL_HEIGHT = 19;

  public static final int CENTER_X = CELL_WIDTH / 2;

  public static final int CENTER_Y = CELL_HEIGHT / 2;

  private RoomGeometry() {
  }

  public static int tileWidth(int cellsAcross) {
    return cellsAcross * CELL_WIDTH;
  }

  public static int tileHeight(int cellsDown) {
    return cellsDown * CELL_HEIGHT;
  }

  public static int cellXOf(int tileX) {
    return Math.floorDiv(tileX, CELL_WIDTH);
  }

  public static int cellYOf(int tileY) {
    return Math.floorDiv(tileY, CELL_HEIGHT);
  }

  public static int originXOf(int cellX) {
    return cellX * CELL_WIDTH;
  }

  public static int originYOf(int cellY) {
    return cellY * CELL_HEIGHT;
  }
}
