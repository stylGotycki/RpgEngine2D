package net.dp.rpg.engine.tile.exception;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TileTypeConflictException extends TileException {

  public TileTypeConflictException(String id, Object existing, Object candidate) {
    super("Tile type '%s' is already registered with different data.%n  existing: %s%n  new:      %s"
        .formatted(id, existing, candidate));
  }

  public static TileTypeConflictException ofProperties(String id, Map<String, Object> existing,
                                                       Map<String, Object> candidate) {
    Set<String> keys = new LinkedHashSet<>(existing.keySet());

    keys.addAll(candidate.keySet());

    StringBuilder differences = new StringBuilder();

    for (String key : keys) {
      Object left = existing.get(key);
      Object right = candidate.get(key);

      if (!java.util.Objects.equals(left, right)) {
        differences.append("%n  %s: %s vs %s".formatted(key, describe(left), describe(right)));
      }
    }

    return new TileTypeConflictException("Tile type '%s' is declared differently by two tilesets:%s"
        .formatted(id, differences));
  }

  private TileTypeConflictException(String message) {
    super(message);
  }

  private static String describe(Object value) {
    return value == null ? "<absent>" : value.toString();
  }
}
