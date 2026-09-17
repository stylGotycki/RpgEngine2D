package net.dp.rpg.engine.tile.exception;

/** A room shape is empty, or its cells do not form one connected group. */
public class InvalidRoomShapeException extends TileException {

  public InvalidRoomShapeException(String message) {
    super(message);
  }
}
