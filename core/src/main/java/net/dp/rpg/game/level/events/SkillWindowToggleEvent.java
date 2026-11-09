package net.dp.rpg.game.level.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class SkillWindowToggleEvent implements GameEvent
{
    public boolean show;
    public float xPos;
    public float yPos;

    @Override
    public void reset()
    {
        show = false;
    }
}
