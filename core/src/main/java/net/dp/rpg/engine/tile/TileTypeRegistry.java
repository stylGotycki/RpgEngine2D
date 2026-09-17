package net.dp.rpg.engine.tile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dp.rpg.engine.tile.exception.TileTypeConflictException;
import net.dp.rpg.engine.tile.exception.UnknownTileTypeException;

public final class TileTypeRegistry {

  public static final int UNKNOWN_RUNTIME_ID = -1;

  private final List<TileType> types = new ArrayList<>();

  private final Map<String, Integer> runtimeIdById = new LinkedHashMap<>();

  public int register(TileType type) {
    if (type == null) {
      throw new IllegalArgumentException("Tile type must not be null");
    }

    Integer existingRuntimeId = runtimeIdById.get(type.id());

    if (existingRuntimeId != null) {
      TileType existingType = types.get(existingRuntimeId);

      if (!existingType.equals(type)) {
        if (existingType.properties().equals(type.properties())) {
          throw new TileTypeConflictException(type.id(), existingType, type);
        }

        throw TileTypeConflictException.ofProperties(
            type.id(), existingType.properties(), type.properties());
      }

      return existingRuntimeId;
    }

    int runtimeId = types.size();

    types.add(type);
    runtimeIdById.put(type.id(), runtimeId);

    return runtimeId;
  }

  public void registerAll(Collection<TileType> newTypes) {
    if (newTypes == null) {
      throw new IllegalArgumentException("Tile types must not be null");
    }

    newTypes.forEach(this::register);
  }

  public TileType require(int runtimeId) {
    if (runtimeId < 0 || runtimeId >= types.size()) {
      throw UnknownTileTypeException.byRuntimeId(runtimeId, types.size());
    }

    return types.get(runtimeId);
  }

  public TileType require(String id) {
    return types.get(requireRuntimeId(id));
  }

  public int requireRuntimeId(String id) {
    Integer runtimeId = runtimeIdById.get(id);

    if (runtimeId == null) {
      throw UnknownTileTypeException.byId(id, runtimeIdById.keySet());
    }

    return runtimeId;
  }

  public int findRuntimeId(String id) {
    return runtimeIdById.getOrDefault(id, UNKNOWN_RUNTIME_ID);
  }

  public Optional<TileType> find(String id) {
    Integer runtimeId = runtimeIdById.get(id);

    return runtimeId == null ? Optional.empty() : Optional.of(types.get(runtimeId));
  }

  public boolean contains(String id) {
    return runtimeIdById.containsKey(id);
  }

  public List<TileType> findByRole(TileRole role) {
    return types.stream()
        .filter(type -> type.role() == role)
        .toList();
  }

  public List<TileType> findByLayer(TileLayerKind layer) {
    return types.stream()
        .filter(type -> type.layer() == layer)
        .toList();
  }

  public List<TileType> findByTag(String tag) {
    return types.stream()
        .filter(type -> type.hasTag(tag))
        .toList();
  }

  public List<TileType> all() {
    return Collections.unmodifiableList(types);
  }

  public Collection<String> ids() {
    return Collections.unmodifiableCollection(runtimeIdById.keySet());
  }

  public int size() {
    return types.size();
  }
}
