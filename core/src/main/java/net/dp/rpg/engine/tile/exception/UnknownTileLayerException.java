package net.dp.rpg.engine.tile.exception;

import java.util.Collection;

public class UnknownTileLayerException extends TileException {

  private UnknownTileLayerException(String message) {
    super(message);
  }

  public static UnknownTileLayerException byKind(Object kind, Collection<String> available) {
    return new UnknownTileLayerException("Map has no %s layer, available: %s".formatted(kind, available));
  }
}
