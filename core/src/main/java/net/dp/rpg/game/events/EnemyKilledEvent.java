package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.enemy.EnemyDef;

public class EnemyKilledEvent implements GameEvent {

  public EnemyDef enemy;

  public TileRoom room;

  @Override
  public void reset() {
    enemy = null;
    room = null;
  }
}
