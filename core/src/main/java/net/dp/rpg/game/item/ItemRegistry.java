package net.dp.rpg.game.item;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemRegistry {

  public static final String PATH = "items/items.json";

  private final Map<String, ItemDef> items = new LinkedHashMap<>();

  private ItemRegistry() {
  }

  public static ItemRegistry parse(String json) {
    ItemRegistry registry = new ItemRegistry();

    for (JsonValue entry : new JsonReader().parse(json).get("items")) {
      ItemDef item = read(entry);

      if (registry.items.put(item.id(), item) != null) {
        throw new IllegalArgumentException("Duplicate item id: " + item.id());
      }
    }

    return registry;
  }

  private static ItemDef read(JsonValue entry) {
    String weapon = entry.getString("weapon", null);
    String potion = entry.getString("potion", null);

    return new ItemDef(
        entry.getString("id"),
        entry.getString("name", entry.getString("id")),
        ItemCategory.valueOf(entry.getString("category")),
        weapon == null ? null : WeaponClass.valueOf(weapon),
        stats(entry.get("bonus")),
        stats(entry.get("requires")),
        entry.getDouble("attackSpeed", 1.0),
        potion == null ? null : PotionEffect.valueOf(potion),
        entry.getBoolean("big", false),
        entry.getInt("tier", 1));
  }

  private static Map<Stat, Integer> stats(JsonValue value) {
    Map<Stat, Integer> stats = new EnumMap<>(Stat.class);

    if (value != null) {
      for (JsonValue stat : value) {
        stats.put(Stat.valueOf(stat.name()), stat.asInt());
      }
    }

    return stats;
  }

  public ItemDef get(String id) {
    ItemDef item = items.get(id);

    if (item == null) {
      throw new IllegalArgumentException("Unknown item: " + id);
    }

    return item;
  }

  public List<ItemDef> all() {
    return List.copyOf(items.values());
  }

  public List<ItemDef> of(ItemCategory category) {
    return items.values().stream().filter(item -> item.category() == category).toList();
  }
}
