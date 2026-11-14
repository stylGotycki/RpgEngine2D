package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class GameMessageEvent implements GameEvent {

  public String text;

  @Override
  public void reset() {
    text = null;
  }
}
