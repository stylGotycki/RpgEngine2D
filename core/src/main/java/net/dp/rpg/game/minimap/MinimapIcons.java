package net.dp.rpg.game.minimap;

import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;

/**
 * Which atlas region marks which room type on the minimap. NORMAL and EMPTY rooms get no icon, so
 * the special rooms stand out. The images are in {@code assets/textures-raw/minimap/} (10x10 px)
 * and go into {@code textures.atlas} through the texture packer.
 */
public final class MinimapIcons {

  public static final String PIXEL = "minimap/pixel";

  public static final String QUEST = "minimap/quest";

  private static final Map<RoomType, String> REGIONS = new EnumMap<>(RoomType.class);

  static {
    REGIONS.put(RoomType.START, "minimap/start");
    REGIONS.put(RoomType.SHOP, "minimap/shop");
    REGIONS.put(RoomType.PUZZLE, "minimap/puzzle");
    REGIONS.put(RoomType.BOSS, "minimap/boss");
    REGIONS.put(RoomType.MINIBOSS, "minimap/miniboss");
    REGIONS.put(RoomType.VAULT, "minimap/vault");
    REGIONS.put(RoomType.COLLECTIBLE, "minimap/collectible");
    REGIONS.put(RoomType.POWER_FIELD, "minimap/power_field");
  }

  private MinimapIcons() {
  }

  /** The atlas region of the icon, or null for a room type without one. */
  public static String regionOf(RoomType type) {
    return REGIONS.get(type);
  }

  public static Map<RoomType, String> all() {
    return Map.copyOf(REGIONS);
  }
}
