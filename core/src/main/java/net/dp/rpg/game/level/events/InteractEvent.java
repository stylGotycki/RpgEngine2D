package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class InteractEvent implements GameEvent {

  public float x;

  public float y;

  @Override
  public void reset() {
    x = 0;
    y = 0;
  }
}
