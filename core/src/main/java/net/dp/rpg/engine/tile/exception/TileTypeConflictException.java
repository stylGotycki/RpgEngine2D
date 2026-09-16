package net.dp.rpg.engine.tile.exception;

public class TileTypeConflictException extends TileException {

  public TileTypeConflictException(String id, Object existing, Object candidate) {
    super("Tile type '%s' is already registered with different data.%n  existing: %s%n  new:      %s"
        .formatted(id, existing, candidate));
  }
}
