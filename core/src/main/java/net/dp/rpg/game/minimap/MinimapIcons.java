package net.dp.rpg.game.minimap;

import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;

public final class MinimapIcons {

  public static final String PIXEL = "minimap/pixel";

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

  public static String regionOf(RoomType type) {
    return REGIONS.get(type);
  }

  public static Map<RoomType, String> all() {
    return Map.copyOf(REGIONS);
  }
}
