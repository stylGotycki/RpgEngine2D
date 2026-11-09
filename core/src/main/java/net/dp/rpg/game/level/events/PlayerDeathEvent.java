package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class PlayerDeathEvent implements GameEvent
{
    public int playerId;

    @Override
    public void reset()
    {
        playerId = -1;
    }
}
