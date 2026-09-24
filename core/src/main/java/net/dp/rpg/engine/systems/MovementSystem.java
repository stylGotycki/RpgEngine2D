package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
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

    public void follow(ComponentStorage<FollowComponent> followComponents, ComponentStorage<TransformComponent> transformComponents, ComponentStorage<BoundingComponent> bounds, float delta)
    {
        int fSize = followComponents.size();
        for(int i = 0; i < fSize; i++)
        {
            FollowComponent followComponent = followComponents.getByIndex(i);
            int following = followComponents.getEntity(i);
            int target = followComponent.targetEntity;
            if(transformComponents.hasComponent(following))
            {
                Vector2 position = transformComponents.getByEntity(following).position;
                Vector2 targetPosition = transformComponents.getByEntity(target).position;
                float targetPositionX = targetPosition.x;
                float targetPositionY = targetPosition.y;
                if(bounds.hasComponent(following))
                {
                    BoundingComponent bound = bounds.getByIndex(i);
                    targetPositionX = MathUtils.clamp(targetPositionX, bound.minX, bound.maxX);
                    targetPositionY = MathUtils.clamp(targetPositionY, bound.minY, bound.maxY);
                }
                position.x += followComponent.lerp * delta * (targetPositionX - position.x);
                position.y += followComponent.lerp * delta * (targetPositionY - position.y);
            }
        }
    }

    public void boundPositions(ComponentStorage<BoundingComponent> bounds, ComponentStorage<TransformComponent> transforms)
    {
        int bSize = bounds.size();
        for(int i = 0; i < bSize; i++)
        {
            int entity = bounds.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                BoundingComponent bound = bounds.getByIndex(i);
                Vector2 position = transforms.getByEntity(entity).position;
                position.x = MathUtils.clamp(position.x, bound.minX, bound.maxX);
                position.y = MathUtils.clamp(position.y, bound.minY, bound.maxY);
            }
        }
    }

    public void bodyToTransform(ComponentStorage<PhysicalBodyComponent> bodies, ComponentStorage<TransformComponent> transforms)
    {
        int bSize = bodies.size();
        for(int i = 0; i < bSize; i++)
        {
            int entity = bodies.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                Body body = bodies.getByIndex(i).body;
                TransformComponent transform = transforms.getByEntity(entity);
                transform.position.set(body.getPosition());
                transform.rotation = body.getTransform().getRotation();
            }
        }
    }

    public void transformToSprite(ComponentStorage<TransformComponent> transforms, ComponentStorage<SpriteComponent> sprites)
    {
        int sSize = sprites.size();
        for(int i = 0; i < sSize; i++)
        {
            int entity = sprites.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                Sprite sprite = sprites.getByIndex(i).sprite;
                TransformComponent transform = transforms.getByEntity(entity);
                sprite.setCenter(transform.position.x, transform.position.y);
                sprite.setRotation(transform.rotation * MathUtils.radDeg);
            }
        }
    }

    public void transformToCamera(ComponentStorage<TransformComponent> transforms, ComponentStorage<CameraComponent> cameras)
    {
        int cSize = cameras.size();
        for(int i = 0; i < cSize; i++)
        {
            int entity = cameras.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                Camera camera = cameras.getByIndex(i).camera;
                TransformComponent transform = transforms.getByEntity(entity);
                camera.position.x = transform.position.x;
                camera.position.y = transform.position.y;
                camera.update();
            }
        }
    }
}
