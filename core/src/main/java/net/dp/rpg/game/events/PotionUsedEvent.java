package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.item.ItemDef;

public class PotionUsedEvent implements GameEvent {

  public ItemDef potion;

  public int slot;

  @Override
  public void reset() {
    potion = null;
    slot = 0;
  }
}
