package net.dp.rpg.engine.tile;

import java.util.Locale;
import net.dp.rpg.engine.tile.exception.InvalidTileDefinitionException;

public enum TileLayerKind {

  GROUND,
  DETAILS,
  OVERLAY;

  public static TileLayerKind from(String value) {
    if (value == null || value.isBlank()) {
      return GROUND;
    }

    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw InvalidTileDefinitionException.unknownEnumValue("tile layer kind", value, exception);
    }
  }
}
