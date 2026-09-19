package net.dp.rpg.engine.tile;

import net.dp.rpg.engine.tile.exception.InvalidTileDefinitionException;

import java.util.Locale;

public enum TileRole {

  FLOOR(true),
  DOOR(true),
  HAZARD(true),
  DECORATION(true),
  WALL(false),
  OBSTACLE(false),
  PIT(false),
  UNSPECIFIED(false);

  private final boolean defaultWalkable;

  TileRole(boolean defaultWalkable) {
    this.defaultWalkable = defaultWalkable;
  }

  public boolean isDefaultWalkable() {
    return defaultWalkable;
  }

  public static TileRole from(String value) {
    if (value == null || value.isBlank()) {
      return UNSPECIFIED;
    }

    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw InvalidTileDefinitionException.unknownEnumValue("tile role", value, exception);
    }
  }
}
