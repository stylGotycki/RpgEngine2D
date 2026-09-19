package net.dp.rpg.engine.tile.exception;

import java.util.Collection;

public class TilesetCoverageException extends TileException {

  public TilesetCoverageException(String tilesetId, Collection<String> missingTypeIds) {
    super("Tileset '%s' does not cover %d tile type(s): %s".formatted(tilesetId, missingTypeIds.size(),
        missingTypeIds));
  }
}
