package net.dp.rpg.engine.tile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import net.dp.rpg.engine.tile.exception.InvalidTileDefinitionException;

public record TileType(String id, TileRole role, TileLayerKind layer, boolean walkable,
                       boolean blocksSight, double defaultWeight, Set<String> tags, Map<String, Object> properties) {

  public TileType {
    if (id == null || id.isBlank()) {
      throw InvalidTileDefinitionException.blankField("Tile type id");
    }

    if (role == null) {
      throw InvalidTileDefinitionException.blankField("Tile role of " + id);
    }

    if (layer == null) {
      throw InvalidTileDefinitionException.blankField("Tile layer of " + id);
    }

    if (!Double.isFinite(defaultWeight) || defaultWeight <= 0.0) {
      throw new InvalidTileDefinitionException("Tile weight must be finite and greater than zero: %s = %s"
          .formatted(id, defaultWeight));
    }

    tags = copyTags(tags);
    properties = copyProperties(properties);
  }

  @Builder(toBuilder = true)
  private static TileType of(String id, TileRole role, TileLayerKind layer, Boolean walkable,
                             Boolean blocksSight, Double defaultWeight, Set<String> tags,
                             Map<String, Object> properties) {
    TileRole resolvedRole = role == null ? TileRole.UNSPECIFIED : role;

    return new TileType(
        id,
        resolvedRole,
        layer == null ? TileLayerKind.GROUND : layer,
        walkable == null ? resolvedRole.isDefaultWalkable() : walkable,
        blocksSight != null && blocksSight,
        defaultWeight == null ? 1.0 : defaultWeight,
        tags,
        properties);
  }

  public boolean hasTag(String tag) {
    return tags.contains(tag);
  }

  private static Set<String> copyTags(Set<String> source) {
    if (source == null || source.isEmpty()) {
      return Set.of();
    }

    return Collections.unmodifiableSet(new LinkedHashSet<>(source));
  }

  private static Map<String, Object> copyProperties(Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }

    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
