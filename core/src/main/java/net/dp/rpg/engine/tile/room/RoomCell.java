package net.dp.rpg.engine.tile.room;

public record RoomCell(int x, int y) {

  public static final RoomCell ORIGIN = new RoomCell(0, 0);

  public RoomCell neighbour(Direction direction) {
    return new RoomCell(x + direction.getDeltaX(), y + direction.getDeltaY());
  }

  public RoomCell translated(int deltaX, int deltaY) {
    return new RoomCell(x + deltaX, y + deltaY);
  }

  public int tileOriginX() {
    return RoomGeometry.originXOf(x);
  }

  public int tileOriginY() {
    return RoomGeometry.originYOf(y);
  }

  public int doorTileX(Direction direction) {
    return switch (direction) {
      case NORTH, SOUTH -> tileOriginX() + RoomGeometry.CENTER_X;
      case WEST -> tileOriginX();
      case EAST -> tileOriginX() + RoomGeometry.CELL_WIDTH - 1;
    };
  }

  public int doorTileY(Direction direction) {
    return switch (direction) {
      case WEST, EAST -> tileOriginY() + RoomGeometry.CENTER_Y;
      case NORTH -> tileOriginY();
      case SOUTH -> tileOriginY() + RoomGeometry.CELL_HEIGHT - 1;
    };
  }
}
