package net.dp.rpg.engine.tile.exception;

public abstract class TileException extends RuntimeException {

  protected TileException(String message) {
    super(message);
  }

  protected TileException(String message, Throwable cause) {
    super(message, cause);
  }
}
