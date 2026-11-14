package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class QuestEvent implements GameEvent {

  public enum Kind {
    ACCEPTED,
    DECLINED,
    GOAL_DONE,
    COMPLETED,
    REWARDED
  }

  public Kind kind;

  public String questId;

  public String template;

  public String goalId;

  @Override
  public void reset() {
    kind = null;
    questId = null;
    template = null;
    goalId = null;
  }
}
