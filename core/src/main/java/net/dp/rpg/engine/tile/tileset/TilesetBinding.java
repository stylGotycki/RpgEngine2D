package net.dp.rpg.engine.tile.tileset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.tile.exception.InvalidTilesetException;

public record TilesetBinding(Map<String, List<Integer>> localIdsByTypeId) {

  public TilesetBinding {
    if (localIdsByTypeId == null || localIdsByTypeId.isEmpty()) {
      throw new InvalidTilesetException("Tileset binding must not be empty");
    }

    localIdsByTypeId = deepCopy(localIdsByTypeId);
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean covers(String tileTypeId) {
    return localIdsByTypeId.containsKey(tileTypeId);
  }

  public List<Integer> variants(String tileTypeId) {
    return localIdsByTypeId.getOrDefault(tileTypeId, List.of());
  }

  public int variantCount(String tileTypeId) {
    return variants(tileTypeId).size();
  }

  public int localId(String tileTypeId) {
    List<Integer> variants = requireVariants(tileTypeId);

    return variants.getFirst();
  }

  public int localId(String tileTypeId, int x, int y, long seed) {
    List<Integer> variants = requireVariants(tileTypeId);

    if (variants.size() == 1) {
      return variants.getFirst();
    }

    return variants.get(variantIndex(variants.size(), x, y, seed));
  }

  public Set<String> coveredTypeIds() {
    return localIdsByTypeId.keySet();
  }

  public List<String> findMissing(Iterable<String> requiredTypeIds) {
    List<String> missing = new ArrayList<>();

    for (String typeId : requiredTypeIds) {
      if (!covers(typeId)) {
        missing.add(typeId);
      }
    }

    return missing;
  }

  public int size() {
    return localIdsByTypeId.size();
  }

  private List<Integer> requireVariants(String tileTypeId) {
    List<Integer> variants = localIdsByTypeId.get(tileTypeId);

    if (variants == null) {
      throw new InvalidTilesetException("Tileset binding has no entry for tile type: " + tileTypeId);
    }

    return variants;
  }

  private static int variantIndex(int variantCount, int x, int y, long seed) {
    long hash = seed * 0x9e3779b97f4a7c15L + x;

    hash = hash * 0x9e3779b97f4a7c15L + y;
    hash ^= hash >>> 33;
    hash *= 0xff51afd7ed558ccdL;
    hash ^= hash >>> 33;

    return Math.floorMod(hash, variantCount);
  }

  private static Map<String, List<Integer>> deepCopy(Map<String, List<Integer>> source) {
    Map<String, List<Integer>> result = new LinkedHashMap<>();

    source.forEach((typeId, localIds) -> {
      if (typeId == null || typeId.isBlank()) {
        throw InvalidTilesetException.blankField("Bound tile type id");
      }

      if (localIds == null || localIds.isEmpty()) {
        throw new InvalidTilesetException("Tile type '%s' has no local ids bound".formatted(typeId));
      }

      result.put(typeId, List.copyOf(localIds));
    });

    return Collections.unmodifiableMap(result);
  }

  public static final class Builder {

    private final Map<String, List<Integer>> localIdsByTypeId = new LinkedHashMap<>();

    private final Set<Integer> usedLocalIds = new LinkedHashSet<>();

    public Builder bind(String tileTypeId, int localId) {
      if (tileTypeId == null || tileTypeId.isBlank()) {
        throw InvalidTilesetException.blankField("Bound tile type id");
      }

      if (localId < 0) {
        throw new InvalidTilesetException("Local tile id must not be negative: " + localId);
      }

      if (!usedLocalIds.add(localId)) {
        throw new InvalidTilesetException("Local tile id %d is bound twice".formatted(localId));
      }

      localIdsByTypeId.computeIfAbsent(tileTypeId, key -> new ArrayList<>()).add(localId);

      return this;
    }

    public TilesetBinding build() {
      return new TilesetBinding(localIdsByTypeId);
    }
  }
}
