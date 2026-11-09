package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class DialogueEndedEvent implements GameEvent {

  public String dialogueId;

  @Override
  public void reset() {
    dialogueId = null;
  }
}
