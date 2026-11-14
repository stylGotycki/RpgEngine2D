package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.TileRoom;

public class PlayerDamagedEvent implements GameEvent {

  public float amount;

  public String source;

  public TileRoom room;

  @Override
  public void reset() {
    amount = 0;
    source = null;
    room = null;
  }
}
