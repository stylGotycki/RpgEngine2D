package net.dp.rpg.engine.tile.exception;

import java.util.Collection;

public class UnknownTileTypeException extends TileException {

  private UnknownTileTypeException(String message) {
    super(message);
  }

  public static UnknownTileTypeException byId(String id) {
    return new UnknownTileTypeException("Unknown tile type id: " + id);
  }

  public static UnknownTileTypeException byId(String id, Collection<String> known) {
    return new UnknownTileTypeException("Unknown tile type id '%s', registered: %s".formatted(id, known));
  }

  public static UnknownTileTypeException byRuntimeId(int runtimeId, int registrySize) {
    return new UnknownTileTypeException("Unknown tile runtime id %d, registry holds %d types"
        .formatted(runtimeId, registrySize));
  }
}
