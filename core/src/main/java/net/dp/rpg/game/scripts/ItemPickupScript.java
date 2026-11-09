package net.dp.rpg.game.scripts;

import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.game.item.ItemDef;

public class ItemPickupScript extends EntityScript {

  private final ItemDef item;

  private final String group;

  public ItemPickupScript(ItemDef item, String group) {
    this.item = item;
    this.group = group;
  }

  public ItemDef item() {
    return item;
  }

  // Pickups sharing a group are alternatives (shop offers): taking one removes the others.
  public String group() {
    return group;
  }

  @Override
  public void update(float delta) {
  }
}
