package net.dp.rpg.engine.tile;

import net.dp.rpg.engine.tile.exception.InvalidTileMapException;

public record TileLayer(String name, TileLayerKind kind, TileGrid grid) {

  public TileLayer {
    if (name == null || name.isBlank()) {
      throw new InvalidTileMapException("Layer name must not be blank");
    }

    if (kind == null) {
      throw new InvalidTileMapException("Layer kind must not be null: " + name);
    }

    if (grid == null) {
      throw new InvalidTileMapException("Layer grid must not be null: " + name);
    }
  }

  public static TileLayer of(TileLayerKind kind, TileGrid grid) {
    return new TileLayer(defaultName(kind), kind, grid);
  }

  public int width() {
    return grid.getWidth();
  }

  public int height() {
    return grid.getHeight();
  }

  private static String defaultName(TileLayerKind kind) {
    if (kind == null) {
      throw new InvalidTileMapException("Layer kind must not be null");
    }

    String lowerCase = kind.name().toLowerCase(java.util.Locale.ROOT);

    return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
  }
}
