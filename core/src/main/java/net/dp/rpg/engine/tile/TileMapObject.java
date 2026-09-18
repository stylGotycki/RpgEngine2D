package net.dp.rpg.engine.tile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TileMapObject(int id, String name, String type, float x, float y, float width, float height,
                            Map<String, Object> properties) {

  public TileMapObject {
    name = name == null ? "" : name;
    type = type == null ? "" : type;
    properties = TileValues.properties(properties);
  }

  public boolean isPoint() {
    return width == 0.0f && height == 0.0f;
  }

  public boolean isType(String candidate) {
    return type.equals(candidate);
  }

}
