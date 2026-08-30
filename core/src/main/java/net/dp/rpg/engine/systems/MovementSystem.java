package net.dp.rpg.engine.systems;

import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.MoveComponent;
import net.dp.rpg.engine.components.TransformComponent;

public class MovementSystem
{
    public void updatePositions(ComponentStorage<TransformComponent> transforms, ComponentStorage<MoveComponent> velocities, float delta)
    {
        int vSize = velocities.size();
        for(int i = 0; i < vSize; i++)
        {
            MoveComponent move = velocities.getByIndex(i);
            if(move.velocity.len() != 0)
            {
                TransformComponent transform = transforms.getByEntity(velocities.getEntity(i));
                transform.position.mulAdd(move.velocity, delta);
            }
        }
    }
}
