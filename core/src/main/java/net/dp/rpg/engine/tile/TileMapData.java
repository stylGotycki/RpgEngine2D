package net.dp.rpg.engine.tile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.dp.rpg.engine.tile.exception.InvalidTileMapException;
import net.dp.rpg.engine.tile.exception.UnknownTileLayerException;

public record TileMapData(List<TileLayer> layers, List<TileMapObject> objects, Map<String, Object> properties) {

  public TileMapData {
    if (layers == null || layers.isEmpty()) {
      throw new InvalidTileMapException("Tile map must contain at least one layer");
    }

    layers = List.copyOf(layers);
    validateLayers(layers);
    objects = objects == null ? List.of() : List.copyOf(objects);
    properties = copyProperties(properties);
  }

  public static TileMapData of(List<TileLayer> layers) {
    return new TileMapData(layers, List.of(), Map.of());
  }

  public int width() {
    return layers.getFirst().width();
  }

  public int height() {
    return layers.getFirst().height();
  }

  public TileLayer requireLayer(String name) {
    for (TileLayer layer : layers) {
      if (layer.name().equals(name)) {
        return layer;
      }
    }

    throw UnknownTileLayerException.byName(name, layerNames());
  }

  public TileLayer requireLayer(TileLayerKind kind) {
    return findLayer(kind).orElseThrow(() -> UnknownTileLayerException.byKind(kind, layerNames()));
  }

  public Optional<TileLayer> findLayer(TileLayerKind kind) {
    return layers.stream()
        .filter(layer -> layer.kind() == kind)
        .findFirst();
  }

  public boolean hasLayer(String name) {
    return layers.stream().anyMatch(layer -> layer.name().equals(name));
  }

  public List<TileMapObject> findObjects(String type) {
    return objects.stream()
        .filter(object -> object.isType(type))
        .toList();
  }

  public int topTileAt(int x, int y) {
    int result = TileGrid.EMPTY;

    for (TileLayer layer : layers) {
      int value = layer.grid().getOrEmpty(x, y);

      if (value != TileGrid.EMPTY) {
        result = value;
      }
    }

    return result;
  }

  public Set<Integer> usedRuntimeIds() {
    Set<Integer> used = new LinkedHashSet<>();

    for (TileLayer layer : layers) {
      TileGrid grid = layer.grid();

      for (int y = 0; y < grid.getHeight(); y++) {
        for (int x = 0; x < grid.getWidth(); x++) {
          int value = grid.get(x, y);

          if (value != TileGrid.EMPTY) {
            used.add(value);
          }
        }
      }
    }

    return used;
  }

  public List<String> layerNames() {
    return layers.stream()
        .map(TileLayer::name)
        .toList();
  }

  private static void validateLayers(List<TileLayer> layers) {
    TileLayer first = layers.getFirst();
    int width = first.width();
    int height = first.height();

    Set<String> seenNames = new LinkedHashSet<>();

    for (TileLayer layer : layers) {
      if (layer.width() != width || layer.height() != height) {
        throw InvalidTileMapException.sizeMismatch(
            layer.name(), layer.width(), layer.height(), width, height);
      }

      if (!seenNames.add(layer.name())) {
        throw InvalidTileMapException.duplicateLayer(layer.name());
      }
    }
  }

  private static Map<String, Object> copyProperties(Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }

    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
