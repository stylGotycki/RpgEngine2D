package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.viewport.Viewport;
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

    public void updateCameraPosition(ComponentStorage<CameraComponent> cameras, ComponentStorage<PhysicalBodyComponent> bodies, ComponentStorage<TransformComponent> transforms)
    {
        int cSize = cameras.size();
        for(int i = 0; i < cSize; i++)
        {
            int entity = cameras.getEntity(i);
            CameraComponent component = cameras.getByIndex(i);
            Camera camera = component.camera;
            Vector2 position;
            if(transforms.hasComponent(entity))
            {
                position = transforms.getByEntity(entity).position;
                camera.position.x = position.x;
                camera.position.y = position.y;
            }
            else if(bodies.hasComponent(entity))
            {
                position = bodies.getByEntity(entity).body.getPosition();
                camera.position.x = position.x;
                camera.position.y = position.y;
            }

            if(component.isBounded)
            {
                clamp(camera.position.x, camera.position.y, camera.viewportWidth, camera.viewportHeight, component.boundingRectangle);
            }
            camera.update();
        }
    }

    // todo follow on transform component
    // todo transform hierarchy: body -> transform -> sprite, camera
    public void follow(ComponentStorage<FollowComponent> followComponents, ComponentStorage<CameraComponent> cameras, ComponentStorage<PhysicalBodyComponent> bodies, float delta)
    {
        int fSize = followComponents.size();
        for(int i = 0; i < fSize; i++)
        {
            FollowComponent followComponent = followComponents.getByIndex(i);
            int following = followComponents.getEntity(i);
            int target = followComponent.targetEntity;
            if(cameras.hasComponent(following))
            {
                CameraComponent camComponent = cameras.getByEntity(following);
                Camera camera = camComponent.camera;
                Vector2 targetPosition = bodies.getByEntity(target).body.getPosition();
                targetPosition = clamp(targetPosition.x, targetPosition.y, camera.viewportWidth, camera.viewportHeight, camComponent.boundingRectangle);
                camera.position.x += followComponent.lerp * delta * (targetPosition.x - camera.position.x);
                camera.position.y += followComponent.lerp * delta * (targetPosition.y - camera.position.y);
            }
        }
    }

    private Vector2 clamp(float x, float y, float width, float height, Rectangle bounds)
    {
        float minX = bounds.x + width/2;
        float minY = bounds.y + height/2;
        float maxX = bounds.width - width/2;
        float maxY = bounds.height - height/2;
        if(x < minX)
        {
            x = minX;
        }
        else if(x > maxX)
        {
            x = maxX;
        }
        if(y < minY)
        {
            y = minY;
        }
        else if(y > maxY)
        {
            y = maxY;
        }
        return new Vector2(x,y);
    }
}
