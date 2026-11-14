package net.dp.rpg.engine.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;

/**
 * The knobs of room content. {@link #difficulty} is the one adaptation (E6) turns: it scales the
 * enemy budget of every room. The rest describes the game's rules and stays fixed.
 *
 * @param difficulty multiplier of every room's enemy count; 1 is the designed game
 * @param enemies enemies a room of each type gets at depth 0 (missing type = none)
 * @param depthBonus extra enemies a NORMAL room gets at the floor's deepest point (linear in depth)
 * @param enemyTypes which plain enemy a spot gets, weighted by how deep the room lies
 * @param enemyDoorClearance enemies keep this many tiles (Chebyshev) from every door, so entering
 *     a room is never an instant hit
 * @param npcDoorClearance the same for NPCs; also the distance a quest giver aims for
 * @param spacing tiles between two spots, relaxed when a small room has no room for it
 * @param potionChance chance of a potion in a NORMAL or EMPTY room outside quests
 * @param shopOffers items laid out in a shop (the player takes one)
 */
public record ContentSettings(
    double difficulty,
    Map<RoomType, Range> enemies,
    double depthBonus,
    List<EnemyWeight> enemyTypes,
    int enemyDoorClearance,
    int npcDoorClearance,
    int spacing,
    double potionChance,
    int shopOffers,
    double healingShare) {

  /** Inclusive range of a count. */
  public record Range(int min, int max) {

    public Range {
      if (min < 0 || max < min) {
        throw new IllegalArgumentException("Bad range " + min + ".." + max);
      }
    }
  }

  /** A plain enemy type, with its weight in the shallowest and in the deepest rooms. */
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

    if (healingShare < 0 || healingShare > 1) {
      throw new IllegalArgumentException("healingShare must be within 0..1: " + healingShare);
    }

    enemies = Map.copyOf(enemies);
    enemyTypes = List.copyOf(enemyTypes);
  }

  public Range enemiesOf(RoomType type) {
    return enemies.getOrDefault(type, new Range(0, 0));
  }

  public ContentSettings withPotionChance(double value) {
    return new ContentSettings(difficulty, enemies, depthBonus, enemyTypes, enemyDoorClearance,
        npcDoorClearance, spacing, value, shopOffers, healingShare);
  }

  public ContentSettings withDifficulty(double value) {
    return new ContentSettings(value, enemies, depthBonus, enemyTypes, enemyDoorClearance,
        npcDoorClearance, spacing, potionChance, shopOffers, healingShare);
  }

  // Share of the room potions that are always healing; 0 keeps the even draw of every effect.
  public ContentSettings withHealingShare(double value) {
    return new ContentSettings(difficulty, enemies, depthBonus, enemyTypes, enemyDoorClearance,
        npcDoorClearance, spacing, potionChance, shopOffers, value);
  }

  public ContentSettings withEnemies(Map<RoomType, Range> value) {
    return new ContentSettings(difficulty, value, depthBonus, enemyTypes, enemyDoorClearance,
        npcDoorClearance, spacing, potionChance, shopOffers, healingShare);
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
        3, 2, 2, 0.15, 3, 0.0);
  }
}
