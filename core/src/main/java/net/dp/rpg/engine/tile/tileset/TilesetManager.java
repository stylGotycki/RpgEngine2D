package net.dp.rpg.engine.tile.tileset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import lombok.Getter;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.InvalidTilesetException;
import net.dp.rpg.engine.tile.exception.TilesetCoverageException;
import net.dp.rpg.engine.tile.exception.UnknownTilesetException;

public final class TilesetManager {

  private final TilesetSource source;

  private final TileTypeRegistry typeRegistry;

  private final Map<String, TilesetDefinition> tilesetsById = new LinkedHashMap<>();

  private final Map<String, TilesetDefinition> tilesetsByPath = new LinkedHashMap<>();

  private final List<Consumer<TilesetDefinition>> changeListeners = new ArrayList<>();

  @Getter
  private String activeTilesetId;

  public TilesetManager(TilesetSource source, TileTypeRegistry typeRegistry) {
    if (source == null) {
      throw new IllegalArgumentException("Tileset loader must not be null");
    }

    if (typeRegistry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.source = source;
    this.typeRegistry = typeRegistry;
  }

  public TilesetDefinition load(String internalPath) {
    String path = normalizePath(internalPath);
    TilesetDefinition cached = tilesetsByPath.get(path);

    if (cached != null) {
      return cached;
    }

    LoadedTileset loaded = source.load(path);
    TilesetDefinition definition = loaded.definition();

    TilesetDefinition conflicting = tilesetsById.get(definition.id());

    if (conflicting != null) {
      throw new InvalidTilesetException("Tileset id '%s' is already loaded from '%s'"
          .formatted(definition.id(), conflicting.sourcePath()));
    }

    for (TileType type : loaded.types()) {
      typeRegistry.register(type);
    }

    tilesetsById.put(definition.id(), definition);
    tilesetsByPath.put(path, definition);

    if (activeTilesetId == null) {
      activeTilesetId = definition.id();
    }

    return definition;
  }

  public TilesetDefinition require(String tilesetId) {
    TilesetDefinition definition = tilesetsById.get(tilesetId);

    if (definition == null) {
      throw UnknownTilesetException.byId(tilesetId, tilesetsById.keySet());
    }

    return definition;
  }

  public Optional<TilesetDefinition> find(String tilesetId) {
    return Optional.ofNullable(tilesetsById.get(tilesetId));
  }

  public boolean isLoaded(String tilesetId) {
    return tilesetId != null && tilesetsById.containsKey(tilesetId);
  }

  public TilesetDefinition requireActive() {
    if (activeTilesetId == null) {
      throw UnknownTilesetException.noActive();
    }

    return require(activeTilesetId);
  }

  public TilesetDefinition activate(String tilesetId, Collection<String> requiredTypeIds) {
    TilesetDefinition definition = require(tilesetId);

    validateCoverage(definition, requiredTypeIds);

    activeTilesetId = tilesetId;
    changeListeners.forEach(listener -> listener.accept(definition));

    return definition;
  }

  public TilesetDefinition activate(String tilesetId) {
    return activate(tilesetId, List.of());
  }

  public void validateCoverage(String tilesetId, Collection<String> requiredTypeIds) {
    validateCoverage(require(tilesetId), requiredTypeIds);
  }

  public List<String> findMissingCoverage(String tilesetId, Collection<String> requiredTypeIds) {
    return require(tilesetId).binding().findMissing(requiredTypeIds);
  }

  public Set<String> toTypeIds(Collection<Integer> runtimeIds) {
    Set<String> typeIds = new LinkedHashSet<>();

    runtimeIds.forEach(runtimeId -> typeIds.add(typeRegistry.require(runtimeId).id()));

    return typeIds;
  }

  public void addChangeListener(Consumer<TilesetDefinition> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener must not be null");
    }

    changeListeners.add(listener);
  }

  public void removeChangeListener(Consumer<TilesetDefinition> listener) {
    changeListeners.remove(listener);
  }

  public List<TilesetDefinition> getLoaded() {
    return List.copyOf(tilesetsById.values());
  }

  public int size() {
    return tilesetsById.size();
  }

  public boolean unload(String tilesetId) {
    TilesetDefinition removed = tilesetsById.remove(tilesetId);

    if (removed == null) {
      return false;
    }

    tilesetsByPath.entrySet().removeIf(entry -> entry.getValue() == removed);

    if (tilesetId.equals(activeTilesetId)) {
      activeTilesetId = tilesetsById.isEmpty() ? null : tilesetsById.keySet().iterator().next();
    }

    return true;
  }

  public void clear() {
    tilesetsById.clear();
    tilesetsByPath.clear();
    activeTilesetId = null;
  }

  private static void validateCoverage(TilesetDefinition definition,
                                       Collection<String> requiredTypeIds) {
    if (requiredTypeIds == null || requiredTypeIds.isEmpty()) {
      return;
    }

    List<String> missing = definition.binding().findMissing(requiredTypeIds);

    if (!missing.isEmpty()) {
      throw new TilesetCoverageException(definition.id(), missing);
    }
  }

  private static String normalizePath(String internalPath) {
    if (internalPath == null || internalPath.isBlank()) {
      throw InvalidTilesetException.blankField("Tileset path");
    }

    String path = internalPath.trim().replace('\\', '/');

    while (path.startsWith("./")) {
      path = path.substring(2);
    }

    return path;
  }
}
