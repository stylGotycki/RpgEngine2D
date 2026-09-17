package net.dp.rpg.engine.tile.tileset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dp.rpg.engine.tile.exception.InvalidTilesetException;

public record TilesetDefinition(String id, String sourcePath, String imagePath, int tileWidth,
                                int tileHeight, int columns, int tileCount, TilesetBinding binding,
                                Map<String, Object> properties) {

  public TilesetDefinition {
    requireText("Tileset id", id);
    requireText("Tileset source path", sourcePath);
    requireText("Tileset image path", imagePath);
    requirePositive("Tile width", tileWidth);
    requirePositive("Tile height", tileHeight);
    requirePositive("Tileset columns", columns);
    requirePositive("Tileset tile count", tileCount);

    if (binding == null) {
      throw new InvalidTilesetException("Tileset binding must not be null: " + id);
    }

    validateBindingRange(id, tileCount, binding);
    properties = copyProperties(properties);
  }

  public int rows() {
    return (tileCount + columns - 1) / columns;
  }

  public int expectedImageWidth() {
    return columns * tileWidth;
  }

  public int expectedImageHeight() {
    return rows() * tileHeight;
  }

  public int columnOf(int localId) {
    return localId % columns;
  }

  public int rowOf(int localId) {
    return localId / columns;
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
}
