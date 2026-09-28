package net.dp.rpg.engine.tile.exception;

public class TileGridBoundsException extends TileException {

  public TileGridBoundsException(int x, int y, int width, int height) {
    super("Cell (%d, %d) is outside grid %dx%d".formatted(x, y, width, height));
  }
}
