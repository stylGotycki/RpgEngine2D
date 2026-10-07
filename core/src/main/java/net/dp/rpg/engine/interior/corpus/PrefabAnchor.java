package net.dp.rpg.engine.interior.corpus;

import java.util.Arrays;
import java.util.Locale;

public enum PrefabAnchor {

  WALL,

  CELL_CENTER,

  ANY;

  public static PrefabAnchor of(String declared) {
    if (declared == null || declared.isBlank()) {
      return ANY;
    }

    try {
      return valueOf(declared.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unknown prefab anchor '%s'; expected one of %s"
          .formatted(declared, Arrays.toString(values())));
    }
  }
}
