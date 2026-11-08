package net.dp.rpg.engine.interior.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.floor.graph.DoorType;

public record DoorPalette(Map<DoorType, Integer> tokenByType) {

  public DoorPalette {
    tokenByType = Collections.unmodifiableMap(new EnumMap<>(tokenByType));
  }

  public static DoorPalette of(TileClasses classes, Map<DoorType, String> classNameByDoorType) {
    Map<DoorType, Integer> tokens = new EnumMap<>(DoorType.class);

    classNameByDoorType.forEach((doorType, className) -> tokens.put(doorType,
        classes.tokenOf(className)));

    return new DoorPalette(tokens);
  }

  public int tokenOf(DoorType doorType) {
    Integer token = tokenByType.get(doorType);

    if (token == null) {
      throw new IllegalArgumentException("This motif declares no tile class for door type " + doorType);
    }

    return token;
  }
}
