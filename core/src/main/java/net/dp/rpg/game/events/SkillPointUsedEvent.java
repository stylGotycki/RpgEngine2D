package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.SkillType;

public class SkillPointUsedEvent implements GameEvent
{
    public SkillType skillType;
    public int level;

    @Override
    public void reset()
    {
        skillType = null;
        level = 0;
    }
}
