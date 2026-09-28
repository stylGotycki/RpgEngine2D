package net.dp.rpg.engine.tile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class TileValues {

  private TileValues() {
  }

  static Map<String, Object> properties(Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return Map.of();
    }

    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  static Set<String> tags(Set<String> source) {
    if (source == null || source.isEmpty()) {
      return Set.of();
    }

    return Collections.unmodifiableSet(new LinkedHashSet<>(source));
  }
}
