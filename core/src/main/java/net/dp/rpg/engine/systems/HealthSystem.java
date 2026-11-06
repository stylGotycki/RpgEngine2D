package net.dp.rpg.engine.systems;

import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.HealthComponent;

public class HealthSystem
{
    public void updateInvincibility(ComponentStorage<HealthComponent> healthComponents, float delta)
    {
        int hSize = healthComponents.size();
        for(int i = 0; i < hSize; i++)
        {
            HealthComponent health = healthComponents.getByIndex(i);
            if(health.invincibilityTime > 0)
                health.invincibilityTime -= delta;
        }
    }
}
