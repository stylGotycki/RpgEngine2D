package net.dp.rpg.engine.tile;

import net.dp.rpg.engine.tile.exception.InvalidTileDefinitionException;

import java.util.Locale;

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
