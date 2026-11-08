package net.dp.rpg.engine.tile.tiled;

public final class TiledAxis {

  private TiledAxis() {
  }

  public static int flipRow(int row, int height) {
    return height - 1 - row;
  }

  public static float flipObjectY(float y, float objectHeight, float mapHeight) {
    return mapHeight - y - objectHeight;
  }
}
