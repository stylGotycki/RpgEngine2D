package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.components.*;

public class MovementSystem
{
    private final Vector2 tempTargetPos = new Vector2();

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
                tempTargetPos.set(targetPosition);
                if(bounds.hasComponent(following))
                {
                    BoundingComponent bound = bounds.getByIndex(i);
                    tempTargetPos.x = MathUtils.clamp(tempTargetPos.x, bound.minX, bound.maxX);
                    tempTargetPos.y = MathUtils.clamp(tempTargetPos.y, bound.minY, bound.maxY);
                }
                lerp(position, followComponent.lerp, delta, tempTargetPos);
            }
        }
    }

    public void boundPositions(ComponentStorage<BoundingComponent> bounds, ComponentStorage<TransformComponent> transforms, float delta)
    {
        int bSize = bounds.size();
        for(int i = 0; i < bSize; i++)
        {
            int entity = bounds.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                BoundingComponent bound = bounds.getByIndex(i);
                Vector2 position = transforms.getByEntity(entity).position;
                tempTargetPos.x = MathUtils.clamp(position.x, bound.minX, bound.maxX);
                tempTargetPos.y = MathUtils.clamp(position.y, bound.minY, bound.maxY);
                if(bound.smoothCorrection)
                {
                    lerp(position, bound.lerp, delta, tempTargetPos);
                }
                else
                {
                    position.set(tempTargetPos);
                }
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
                if(!transform.changed)
                {
                    transform.position.set(body.getPosition());
                    transform.rotation = body.getTransform().getRotation();
                }
            }
        }
    }

    public void transformToBody(ComponentStorage<PhysicalBodyComponent> bodies, ComponentStorage<TransformComponent> transforms)
    {
        int bSize = bodies.size();
        for(int i = 0; i < bSize; i++)
        {
            int entity = bodies.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                TransformComponent transform = transforms.getByEntity(entity);
                if(transform.changed)
                {
                    Body body = bodies.getByIndex(i).body;
                    body.setTransform(transform.position, transform.rotation);
                    transform.changed = false;
                }
            }
        }
    }

    public void transformToSprite(ComponentStorage<TransformComponent> transforms, ComponentStorage<SpriteComponent> spriteComponents)
    {
        int sSize = spriteComponents.size();
        for(int i = 0; i < sSize; i++)
        {
            int entity = spriteComponents.getEntity(i);
            if(transforms.hasComponent(entity))
            {
                SpriteComponent spriteComponent = spriteComponents.getByIndex(i);
                for(int j = 0; j < spriteComponent.sprites.size; j++)
                {
                    Array<Sprite> sprites = spriteComponent.sprites;
                    TransformComponent transform = transforms.getByEntity(entity);
                    sprites.get(j).setOriginBasedPosition(transform.position.x, transform.position.y);
                    if(!(spriteComponent.fixedRotation.size > j && spriteComponent.fixedRotation.get(j)))
                        sprites.get(j).setRotation(transform.rotation * MathUtils.radDeg);
                }
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

    public void transformToLight(ComponentStorage<TransformComponent> transforms, ComponentStorage<LightComponent> lights)
    {
        int lSize = lights.size();
        for(int i = 0; i < lSize; i++)
        {
            int entity = lights.getEntity(i);
            LightComponent lightComponent = lights.getByEntity(entity);
            if (lightComponent.light.getBody() == null && transforms.hasComponent(entity))
            {
                TransformComponent transform = transforms.getByEntity(entity);
                lightComponent.light.setPosition(transform.position);
            }
        }
    }

    private void lerp(Vector2 position, float lerp, float delta, Vector2 target)
    {
        position.x += lerp * delta * (target.x - position.x);
        position.y += lerp * delta * (target.y - position.y);
    }
}
