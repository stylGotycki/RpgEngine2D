package net.dp.rpg.game.level;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;

public class HealthChangeEvent implements GameEvent
{
    public float currentHealth;

    @Override
    public void reset()
    {
        currentHealth = 0;
    }
}
