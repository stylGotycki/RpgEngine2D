package net.dp.rpg.game.item;

import com.badlogic.gdx.Gdx;

public final class GameItems {

  private static ItemRegistry registry;

  private static LootTable loot;

  private GameItems() {
  }

  public static synchronized ItemRegistry registry() {
    if (registry == null) {
      registry = ItemRegistry.parse(Gdx.files.internal(ItemRegistry.PATH).readString("UTF-8"));
      loot = new LootTable(registry);
    }

    return registry;
  }

  public static LootTable loot() {
    registry();
    return loot;
  }
}
