package net.dp.rpg.engine.content;

/**
 * Tags the generator writes on spots. Enemy tags are types; item tags are pools the game resolves
 * to a concrete item with its own random stream, so the item list can change (E2) without moving
 * any spot.
 */
public final class ContentTags {

  public static final String SPIDER = "spider";

  public static final String SKELETON = "skeleton";

  public static final String MINIBOSS = "miniboss";

  public static final String BOSS = "boss";

  /** A weapon, armour or ring: the reward of a vault. */
  public static final String EQUIPMENT = "equipment";

  /** Equipment or a potion: the find of a collectible room. */
  public static final String LOOT = "loot";

  public static final String POTION = "potion";

  /** One of the shop's free offers; the player takes one of them (E2). */
  public static final String SHOP_OFFER = "shop-offer";

  public static final String QUEST_GIVER = "quest-giver";

  public static final String TALKER_PREFIX = "talker:";

  public static final String QUEST_ITEM_PREFIX = "quest:";

  public static String talker(String dialogueId) {
    return TALKER_PREFIX + dialogueId;
  }

  public static String questItem(String itemId) {
    return QUEST_ITEM_PREFIX + itemId;
  }

  private ContentTags() {
  }
}
