package net.dp.rpg.engine.tile.tileset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.dp.rpg.engine.tile.exception.InvalidTilesetException;

public record TilesetDefinition(String id, String sourcePath, String imagePath, int tileWidth,
                                int tileHeight, int columns, int tileCount, int spacing, int margin,
                                TilesetBinding binding, Map<String, Object> properties) {

  public TilesetDefinition {
    requireText("Tileset id", id);
    requireText("Tileset source path", sourcePath);
    requireText("Tileset image path", imagePath);
    requirePositive("Tile width", tileWidth);
    requirePositive("Tile height", tileHeight);
    requirePositive("Tileset columns", columns);
    requirePositive("Tileset tile count", tileCount);
    requireNonNegative("Tileset spacing", spacing);
    requireNonNegative("Tileset margin", margin);

    if (binding == null) {
      throw new InvalidTilesetException("Tileset binding must not be null: " + id);
    }

    validateBindingRange(id, tileCount, binding);
    properties = copyProperties(properties);
  }

  private int rows() {
    return (tileCount + columns - 1) / columns;
  }

  public int expectedImageWidth() {
    return margin * 2 + columns * tileWidth + Math.max(0, columns - 1) * spacing;
  }

  public int expectedImageHeight() {
    int rows = rows();

    return margin * 2 + rows * tileHeight + Math.max(0, rows - 1) * spacing;
  }

  private int columnOf(int localId) {
    return localId % columns;
  }

  private int rowOf(int localId) {
    return localId / columns;
  }

  public int pixelXOf(int localId) {
    return margin + columnOf(localId) * (tileWidth + spacing);
  }

  public int pixelYOf(int localId) {
    return margin + rowOf(localId) * (tileHeight + spacing);
  }

  private static void validateBindingRange(String id, int tileCount, TilesetBinding binding) {
    binding.localIdsByTypeId().forEach((typeId, localIds) ->
        localIds.forEach(localId -> {
          if (localId >= tileCount) {
            throw InvalidTilesetException.localIdOutOfRange(id, typeId, localId, tileCount);
          }
        }));
  }

  private static Map<String, Object> copyProperties(Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }

    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  private static void requireText(String field, String value) {
    if (value == null || value.isBlank()) {
      throw InvalidTilesetException.blankField(field);
    }
  }

  private static void requirePositive(String field, int value) {
    if (value <= 0) {
      throw InvalidTilesetException.nonPositive(field, value);
    }
  }

  private static void requireNonNegative(String field, int value) {
    if (value < 0) {
      throw new InvalidTilesetException("%s must not be negative, got %d".formatted(field, value));
    }
  }
}
