package net.dp.rpg.game.scripts;

import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.game.item.ItemDef;

public class ItemPickupScript extends EntityScript {

  private final ItemDef item;

  private final String group;

  private final String questId;

  private String lockedMessage;

  public ItemPickupScript(ItemDef item, String group, String questId) {
    this.item = item;
    this.group = group;
    this.questId = questId;
  }

  public String questId() {
    return questId;
  }

  public void lock(String message) {
    this.lockedMessage = message;
  }

  public void unlock() {
    lockedMessage = null;
  }

  public boolean isLocked() {
    return lockedMessage != null;
  }

  public String lockedMessage() {
    return lockedMessage;
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
