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
import net.dp.rpg.game.animations.EnemyAnimationId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;

import static net.dp.rpg.game.FixtureCategory.*;

public class SkeletonScript extends EnemyScript
{
    private final float viewAngleDeg = 90;
    private final Vector2 target = new Vector2();
    private final Vector2 currentDirection = new Vector2(1,0);
    private final Vector2 targetDirection = new Vector2();
    private final Vector2 toPlayer = new Vector2();
    private boolean walking = false;
    private final float lookAroundChance = 0.5f;

    public SkeletonScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home, DialogueController dialogues) {
        super(playerEntity, enemy, floor, home, dialogues);
    }

    @Override
    public void enemyUpdate(float delta)
    {
        currentDirection.setLength(1).setAngleRad(getComponent(TransformComponent.class).rotation);
        Vector2 position = getComponent(TransformComponent.class).position;

        Vector2 playerPosition = ((TransformComponent)getScene().getComponent(playerEntity, TransformComponent.class)).position;
        toPlayer.set(playerPosition).sub(position);
        //check if target is out of skeleton sight
        if(toPlayer.dot(currentDirection) > MathUtils.cosDeg(viewAngleDeg/2)
            && !getScene().rayCastCategory(position, playerPosition, WALL, OBSTACLE))
        {
            target.set(playerPosition);
        }

        float rotateSpeed = enemy.acceleration() * delta * 10;

        if(target.dst(position) > 0.3)
        {
            targetDirection.set(target).sub(position).setLength(1);
            currentDirection.rotateDeg(currentDirection.crs(targetDirection) > 0 ? rotateSpeed : -rotateSpeed);
            Body body = getComponent(PhysicalBodyComponent.class).body;
            body.applyForce(currentDirection.scl(getSpeed()), body.getPosition(), true);
            getComponent(TransformComponent.class).setRotation(currentDirection.angleRad());
            if(!walking)
            {
                getComponent(AnimationComponent.class).animationSet.play(EnemyAnimationId.WALK);
                walking = true;
            }
        }
        else
        {
            if(walking)
            {
                getComponent(AnimationComponent.class).animationSet.stop();
                walking = false;
            }
            if(MathUtils.randomBoolean(Utils.getFrameChance(lookAroundChance, delta)))
            {
                targetDirection.setAngleDeg(MathUtils.random(360f));
            }
            currentDirection.rotateDeg(currentDirection.crs(targetDirection) > 0 ? rotateSpeed : -rotateSpeed);
            getComponent(TransformComponent.class).setRotation(currentDirection.angleRad());
        }
    }
}
