package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class NpcActionEvent implements GameEvent {

  public String questId;

  public String action;

  public String npcName;

  @Override
  public void reset() {
    questId = null;
    action = null;
    npcName = null;
  }
}
