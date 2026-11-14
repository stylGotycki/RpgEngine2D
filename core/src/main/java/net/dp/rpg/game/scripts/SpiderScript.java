package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.AnimationComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.animations.SpiderAnimationId;

import static net.dp.rpg.game.FixtureCategory.*;

public class SpiderScript extends EntityScript
{
    private int playerDetected = -1;
    private final Vector2 target = new Vector2();
    private final Vector2 direction = new Vector2();
    private boolean isWalking = false;

    @Override
    public void update(float delta)
    {
        float speed = 10;
        Vector2 position = getComponent(TransformComponent.class).position;
        if(playerDetected >= 0)
        {
            TransformComponent transform = (TransformComponent) getScene().getComponent(playerDetected, TransformComponent.class);
            target.set(transform.position);
        }
        else if(MathUtils.random(1000) == 0)
        {
            target.set(position).add(MathUtils.random(-5,5), MathUtils.random(-5,5));
        }
        if(target.dst(position) > 0.3)
        {
            direction.set(target).sub(position).setLength(1);
            getComponent(TransformComponent.class).setRotation(direction.angleRad());
            Body body = getComponent(PhysicalBodyComponent.class).body;
            body.applyForce(direction.scl(speed), body.getPosition(), true);
            if(!isWalking)
            {
                getComponent(AnimationComponent.class).animationSet.play(SpiderAnimationId.WALK);
                isWalking = true;
            }
        }
        else if(isWalking)
        {
            getComponent(AnimationComponent.class).animationSet.stop();
            isWalking = false;
        }
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, PLAYER) && isCategory(category, SENSOR))
        {
            playerDetected = otherEntity;
        }
    }

    @Override
    public void onEndCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, PLAYER) && isCategory(category, SENSOR))
        {
            playerDetected = -1;
        }
    }

    @Override
    public void onDeath()
    {
        getScene().setToDelete(getEntity());
    }
}
