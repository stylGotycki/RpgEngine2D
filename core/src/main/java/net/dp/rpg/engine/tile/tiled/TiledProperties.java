package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.utils.XmlReader.Element;
import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TiledProperties {

  public static final String TILE_ID = "tileId";

  public static final String ROLE = "role";

  public static final String GENERATION_LAYER = "generationLayer";

  public static final String WALKABLE = "walkable";

  public static final String BLOCKS_SIGHT = "blocksSight";

  public static final String TAGS = "tags";

  public static final String WEIGHT = "wfcWeight";

  private static final TiledProperties EMPTY = new TiledProperties(Map.of());

  private final Map<String, Object> values;

  private static final Set<String> CONSUMED = Set.of(
      TILE_ID, ROLE, GENERATION_LAYER, WALKABLE, BLOCKS_SIGHT, TAGS, WEIGHT);

  private TiledProperties(Map<String, Object> values) {
    this.values = values;
  }

  public static TiledProperties read(Element owner) {
    Element block = owner == null ? null : owner.getChildByName("properties");

    if (block == null) {
      return EMPTY;
    }

    Map<String, Object> values = new LinkedHashMap<>();

    for (Element property : block.getChildrenByName("property")) {
      String name = property.getAttribute("name", null);

      if (name == null || name.isBlank()) {
        throw new InvalidTiledFormatException("Tiled property without a name");
      }

      if (values.containsKey(name)) {
        throw new InvalidTiledFormatException("Duplicate Tiled property: " + name);
      }

      values.put(name, parseValue(name, property));
    }

    return new TiledProperties(Collections.unmodifiableMap(values));
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public String getString(String name) {
    Object value = values.get(name);

    if (value == null) {
      return null;
    }

    if (!(value instanceof String text)) {
      throw InvalidTiledFormatException.wrongPropertyType(name, "a string", value);
    }

    return text;
  }

  public String requireString(String name, String context) {
    String value = getString(name);

    if (value == null || value.isBlank()) {
      throw new InvalidTiledFormatException("%s has no required '%s' property".formatted(context, name));
    }

    return value;
  }

  public Boolean getBoolean(String name) {
    Object value = values.get(name);

    if (value == null) {
      return null;
    }

    if (!(value instanceof Boolean flag)) {
      throw InvalidTiledFormatException.wrongPropertyType(name, "a boolean", value);
    }

    return flag;
  }

  public Double getDouble(String name) {
    Object value = values.get(name);

    if (value == null) {
      return null;
    }

    if (!(value instanceof Number number)) {
      throw InvalidTiledFormatException.wrongPropertyType(name, "a number", value);
    }

    return number.doubleValue();
  }

  public Set<String> getTags(String name) {
    String raw = getString(name);

    if (raw == null || raw.isBlank()) {
      return Set.of();
    }

    Set<String> tags = new LinkedHashSet<>();

    Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .forEach(tags::add);

    return tags;
  }

  public Map<String, Object> asMap() {
    return values;
  }

  private static Object parseValue(String name, Element property) {
    String type = property.getAttribute("type", "string");
    String raw = rawValue(property);

    try {
      return switch (type) {
        case "bool" -> parseBoolean(name, raw);
        case "int", "object" -> Integer.parseInt(raw.trim());
        case "float" -> Double.parseDouble(raw.trim());
        default -> raw;
      };
    } catch (NumberFormatException exception) {
      throw new InvalidTiledFormatException("Invalid value of property '%s': %s".formatted(name, raw), exception);
    }
  }

  public Map<String, Object> extras() {
    Map<String, Object> extras = new LinkedHashMap<>();

    values.forEach((name, value) -> {
      if (!CONSUMED.contains(name)) {
        extras.put(name, value);
      }
    });

    return Collections.unmodifiableMap(extras);
  }

  private static String rawValue(Element property) {
    String attribute = property.getAttribute("value", null);

    if (attribute != null) {
      return attribute;
    }

    String text = property.getText();

    return text == null ? "" : text;
  }

  private static boolean parseBoolean(String name, String raw) {
    if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) {
      return true;
    }

    if ("false".equalsIgnoreCase(raw) || "0".equals(raw)) {
      return false;
    }

    throw new InvalidTiledFormatException("Invalid boolean value of property '%s': %s".formatted(name, raw));
  }
}
