package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class SkillPointsAvailableEvent implements GameEvent
{
    public int skillPoints;

    @Override
    public void reset()
    {
        skillPoints = 0;
    }
}
