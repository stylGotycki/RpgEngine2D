package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Utils;
import net.dp.rpg.engine.components.AnimationComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.animations.SpiderAnimationId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.item.PlayerStats;

import static net.dp.rpg.game.FixtureCategory.*;

public class SpiderScript extends EnemyScript
{
    private int playerDetected = -1;
    private final Vector2 target = new Vector2();
    private final Vector2 targetDirection = new Vector2();
    private final Vector2 currentDirection = new Vector2(1,0);
    private final Vector2 frontRayEnd = new Vector2();
    private final Vector2 leftRayEnd = new Vector2();
    private final Vector2 rightRayEnd = new Vector2();
    private final float changeTargetChance = 0.25f;
    private final float maxMoveDistance = 5f;
    private boolean walking = false;

    public SpiderScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home, DialogueController dialogues)
    {
        super(playerEntity, enemy, floor, home, dialogues);
    }

    @Override
    public void enemyUpdate(float delta)
    {
        PlayerStats playerStats = playerStats();

        float speed = getSpeed();
        Vector2 position = getComponent(TransformComponent.class).position;

        // AnimationSet only advances after play() (dev).
        if(playerDetected >= 0 && playerStats != null && !playerStats.isInvisible())
        {
            TransformComponent transform = (TransformComponent) getScene().getComponent(playerDetected, TransformComponent.class);
            target.set(transform.position);
        }
        else if(MathUtils.randomBoolean(Utils.getFrameChance(changeTargetChance, delta)))
        {
            setRandomTarget();
        }
        if(target.dst(position) > 0.3)
        {
            targetDirection.set(target).sub(position).setLength(1);
            float rotateSpeed = enemy.acceleration() * delta * 10;
            leftRayEnd.set(currentDirection).rotateDeg(-45).setLength(enemy.size()).add(position);
            rightRayEnd.set(currentDirection).rotateDeg(45).setLength(enemy.size()).add(position);
            if(getScene().rayCastCategory(position, leftRayEnd, WALL, OBSTACLE))
            {
                targetDirection.set(currentDirection).rotateDeg(rotateSpeed);
            }
            else if(getScene().rayCastCategory(position, rightRayEnd, WALL, OBSTACLE))
            {
                targetDirection.set(currentDirection).rotateDeg(-rotateSpeed);
            }
            else
            {
                currentDirection.rotateDeg(currentDirection.crs(targetDirection) > 0 ? rotateSpeed : -rotateSpeed);
                targetDirection.set(currentDirection);
            }
            getComponent(TransformComponent.class).setRotation(targetDirection.angleRad());
            currentDirection.set(targetDirection);
            Body body = getComponent(PhysicalBodyComponent.class).body;
            body.applyForce(targetDirection.scl(speed), body.getPosition(), true);
            if(!walking)
            {
                getComponent(AnimationComponent.class).animationSet.play(SpiderAnimationId.WALK);
                walking = true;
            }
            else if(playerDetected < 0)
            {
                frontRayEnd.set(targetDirection).setLength(enemy.size()/1.5f).add(position);
                if(getScene().rayCastCategory(position, frontRayEnd, WALL, OBSTACLE))
                    target.set(position);
            }
        }
        else
        {
            target.set(position);
            if(walking)
            {
                getComponent(AnimationComponent.class).animationSet.stop();
                walking = false;
            }
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

    private void setRandomTarget()
    {
        target.set(getComponent(TransformComponent.class).position).add(MathUtils.random(-maxMoveDistance,maxMoveDistance), MathUtils.random(-maxMoveDistance,maxMoveDistance));
    }
}
