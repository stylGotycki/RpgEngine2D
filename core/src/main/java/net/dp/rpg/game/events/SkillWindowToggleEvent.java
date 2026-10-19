package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class SkillWindowToggleEvent implements GameEvent
{
    public boolean show;

    @Override
    public void reset()
    {
        show = false;
    }
}
