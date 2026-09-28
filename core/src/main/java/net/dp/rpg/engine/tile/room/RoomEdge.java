package net.dp.rpg.engine.tile.room;

public record RoomEdge(RoomCell cell, Direction direction) {

  public int doorTileX() {
    return cell.doorTileX(direction);
  }

  public int doorTileY() {
    return cell.doorTileY(direction);
  }
}
