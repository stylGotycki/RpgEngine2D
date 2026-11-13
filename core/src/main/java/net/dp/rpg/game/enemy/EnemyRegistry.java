package net.dp.rpg.game.enemy;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnemyRegistry {

  public static final String PATH = "enemies/enemies.json";

  private final Map<String, EnemyDef> enemies = new LinkedHashMap<>();

  private EnemyRegistry() {
  }

  public static EnemyRegistry parse(String json) {
    EnemyRegistry registry = new EnemyRegistry();

    for (JsonValue entry : new JsonReader().parse(json).get("enemies")) {
      EnemyDef enemy = new EnemyDef(
          entry.getString("id"),
          entry.getString("name", entry.getString("id")),
          entry.getString("few", entry.getString("id")),
          entry.getString("many", entry.getString("id")),
          entry.getFloat("health"),
          entry.getFloat("damage"),
          entry.getFloat("acceleration"),
          entry.getFloat("size"),
          entry.getString("sprite"),
          entry.getFloat("frameTime", 0.1f),
          entry.getFloat("spriteScale", 1.6f),
          entry.getBoolean("boss", false));

      if (registry.enemies.put(enemy.id(), enemy) != null) {
        throw new IllegalArgumentException("Duplicate enemy id: " + enemy.id());
      }
    }

    return registry;
  }

  public EnemyDef get(String id) {
    EnemyDef enemy = enemies.get(id);

    if (enemy == null) {
      throw new IllegalArgumentException("Unknown enemy: " + id);
    }

    return enemy;
  }

  public boolean has(String id) {
    return enemies.containsKey(id);
  }

  public List<EnemyDef> all() {
    return List.copyOf(enemies.values());
  }
}
