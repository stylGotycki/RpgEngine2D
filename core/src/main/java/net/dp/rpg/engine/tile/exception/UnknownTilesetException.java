package net.dp.rpg.engine.tile.exception;

import java.util.Collection;

public class UnknownTilesetException extends TileException {

  private UnknownTilesetException(String message) {
    super(message);
  }

  public static UnknownTilesetException byId(String tilesetId, Collection<String> loaded) {
    return new UnknownTilesetException("Tileset '%s' is not loaded, available: %s".formatted(tilesetId, loaded));
  }

  public static UnknownTilesetException noActive() {
    return new UnknownTilesetException("No active tileset has been selected");
  }
}
