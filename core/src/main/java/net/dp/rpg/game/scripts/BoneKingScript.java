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
import net.dp.rpg.game.appearanceEnums.EnemyAnimationId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.enemy.GameEnemies;
import net.dp.rpg.game.level.entities.EnemyEntityCreator;

public class BoneKingScript extends EnemyScript
{
    private final EnemyEntityCreator enemyCreator;
    private final Vector2 targetDirection = new Vector2(1,0);
    private final Vector2 currentDirection = new Vector2(1,0);
    private boolean walking = false;
    private final float spawnSkeletonChance = 0.2f;
    private float spawnSkeletonTime = 0f;
    private boolean spawning = false;

    public BoneKingScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home, DialogueController dialogues, EnemyEntityCreator enemyCreator)
    {
        super(playerEntity, enemy, floor, home, dialogues);
        this.enemyCreator = enemyCreator;
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public void enemyUpdate(float delta)
    {
        currentDirection.setLength(1).setAngleRad(getComponent(TransformComponent.class).rotation);
        Vector2 playerPosition = getScene().getComponent(playerEntity, TransformComponent.class).position;
        Vector2 position = getComponent(TransformComponent.class).position;
        if(!spawning && MathUtils.randomBoolean(Utils.getFrameChance(spawnSkeletonChance, delta)))
        {
            spawning = true;
            if(spawnSkeletonTime == 0)
                spawnSkeletonTime = 1;
        }
        else if(spawning && spawnSkeletonTime > 0)
        {
            spawnSkeletonTime -= delta;
            if(spawnSkeletonTime <= 0)
            {
                spawnSkeletonTime = 0;
                spawning = false;
                int amount = MathUtils.random(1,2);
                for(int i = 0; i < amount; i++)
                {
                    enemyCreator.create(GameEnemies.registry().get("skeleton"), position.x, position.y, floor, home);
                }
            }
        }
        else if(playerPosition.dst(position) > 0.3 && !playerStats().isInvisible())
        {
            float rotateSpeed = enemy.acceleration() * delta * 10;
            targetDirection.set(playerPosition).sub(position).setLength(1);
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
        else if(walking)
        {
            getComponent(AnimationComponent.class).animationSet.stop();
            walking = false;
        }
    }

    @Override
    public void onDamage()
    {
        spawning = false;
    }
}
