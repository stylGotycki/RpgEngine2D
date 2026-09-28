package net.dp.rpg.engine.systems;

import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.components.*;

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

    public void syncBodyAndSpritePositions(ComponentStorage<PhysicalBodyComponent> bodies, ComponentStorage<SpriteComponent> sprites)
    {
        int bSize = bodies.size();
        for(int i = 0; i < bSize; i++)
        {
            int entity = bodies.getEntity(i);
            SpriteComponent sprite = sprites.getByEntity(entity);
            if(sprite != null)
            {
                Body body = bodies.getByIndex(i).body;
                sprite.sprite.setCenter(body.getPosition().x, body.getPosition().y);
                sprite.sprite.setRotation(body.getTransform().getRotation() * 180 / (float) Math.PI);
            }
        }
    }
}
