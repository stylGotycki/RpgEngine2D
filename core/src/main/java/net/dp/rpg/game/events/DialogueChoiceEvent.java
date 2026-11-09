package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class DialogueChoiceEvent implements GameEvent {

  public String dialogueId;

  public String nodeId;

  public int choice;

  public String action;

  @Override
  public void reset() {
    dialogueId = null;
    nodeId = null;
    choice = 0;
    action = null;
  }
}
