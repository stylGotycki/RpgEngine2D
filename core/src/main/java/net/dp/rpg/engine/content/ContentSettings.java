package net.dp.rpg.engine.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;

public record ContentSettings(
    double difficulty,
    Map<RoomType, Range> enemies,
    double depthBonus,
    List<EnemyWeight> enemyTypes,
    int enemyDoorClearance,
    int npcDoorClearance,
    int spacing,
    double potionChance,
    int shopOffers) {

  public record Range(int min, int max) {

    public Range {
      if (min < 0 || max < min) {
        throw new IllegalArgumentException("Bad range " + min + ".." + max);
      }
    }
  }

  public record EnemyWeight(String tag, double shallow, double deep) {

    public double at(double depthFraction) {
      return shallow + (deep - shallow) * depthFraction;
    }
  }

  public static final ContentSettings DEFAULTS = defaults();

  public ContentSettings {
    if (difficulty < 0) {
      throw new IllegalArgumentException("difficulty must not be negative: " + difficulty);
    }

    enemies = Map.copyOf(enemies);
    enemyTypes = List.copyOf(enemyTypes);
  }

  public Range enemiesOf(RoomType type) {
    return enemies.getOrDefault(type, new Range(0, 0));
  }

  public ContentSettings withDifficulty(double value) {
    return new ContentSettings(value, enemies, depthBonus, enemyTypes, enemyDoorClearance,
        npcDoorClearance, spacing, potionChance, shopOffers);
  }

  private static ContentSettings defaults() {
    Map<RoomType, Range> enemies = new EnumMap<>(RoomType.class);

    // Rooms without enemies (START, SHOP, PUZZLE, EMPTY) are simply missing.
    enemies.put(RoomType.NORMAL, new Range(2, 3));
    enemies.put(RoomType.POWER_FIELD, new Range(1, 2));
    enemies.put(RoomType.VAULT, new Range(0, 1));
    enemies.put(RoomType.COLLECTIBLE, new Range(0, 1));
    // Besides the miniboss or the boss itself.
    enemies.put(RoomType.MINIBOSS, new Range(2, 3));
    enemies.put(RoomType.BOSS, new Range(3, 4));

    return new ContentSettings(1.0, enemies, 2.0,
        List.of(new EnemyWeight(ContentTags.SPIDER, 0.85, 0.35),
            new EnemyWeight(ContentTags.SKELETON, 0.15, 0.65)),
        3, 2, 2, 0.15, 3);
  }
}
