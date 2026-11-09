package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.item.ItemDef;

public class ItemPickedEvent implements GameEvent {

  public ItemDef item;

  public boolean fromShop;

  @Override
  public void reset() {
    item = null;
    fromShop = false;
  }
}
