package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.Utils;
import net.dp.rpg.engine.components.AnimationComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.animations.SpiderAnimationId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.level.events.EnemyKilledEvent;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.item.PlayerStats;

import static net.dp.rpg.game.FixtureCategory.*;

public class EnemyScript extends EntityScript
{
    private final int playerEntity;
    private int playerDetected = -1;
    private final Vector2 target = new Vector2();
    private final Vector2 targetDirection = new Vector2();
    private final Vector2 currentDirection = new Vector2(1,0);
    private final Vector2 frontRayEnd = new Vector2();
    private final Vector2 leftRayEnd = new Vector2();
    private final Vector2 rightRayEnd = new Vector2();
    private final float changeTargetChance = 0.25f;
    private final float maxMoveDistance = 5f;
    private final TileFloor floor;
    private final TileRoom home;
    private final EnemyDef enemy;
    private boolean dead = false;
    private boolean walking = false;
    private final DialogueController dialogues;

    public EnemyScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home, DialogueController dialogues)
    {
        this.dialogues = dialogues;
        this.playerEntity = playerEntity;
        this.enemy = enemy;
        this.floor = floor;
        this.home = home;
    }

    public EnemyDef getEnemy()
    {
        return enemy;
    }

    @Override
    public void update(float delta)
    {
        // Enemies of other rooms sleep; otherwise they would walk into walls towards the player.
        if(floor != null && home != null && floor.getActiveRoom() != home)
            return;
        // The world waits while the player reads.
        if(dialogues != null && dialogues.isOpen())
            return;
        PlayerStats playerStats = playerStats();
        if(playerStats != null && playerStats.isInvisible())
            return;

        // Acceleration, not force, so a heavy boss is not slower than its numbers say.
        float speed = enemy.acceleration() * getComponent(PhysicalBodyComponent.class).body.getMass();
        Vector2 position = getComponent(TransformComponent.class).position;

        // AnimationSet only advances after play() (dev).
        if(playerDetected >= 0)
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
            if(playerDetected >= 0)
            {
                leftRayEnd.set(currentDirection).rotateDeg(-45).setLength(enemy.size()).add(position);
                rightRayEnd.set(currentDirection).rotateDeg(45).setLength(enemy.size()).add(position);
                if(isCategory(getScene().rayCastCategory(position, leftRayEnd), WALL, OBSTACLE))
                {
                    targetDirection.set(currentDirection).rotateDeg(5);
                }
                else if(isCategory(getScene().rayCastCategory(position, rightRayEnd), WALL, OBSTACLE))
                {
                    targetDirection.set(currentDirection).rotateDeg(-5);
                }
                else
                {
                    currentDirection.rotateDeg(currentDirection.crs(targetDirection) > 0 ? 5 : -5);
                    targetDirection.set(currentDirection);
                }
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
                if(isCategory(getScene().rayCastCategory(position, frontRayEnd), WALL, OBSTACLE))
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

    // CombatSystem deals the player's sword damage (AttackComponent); this only reacts to dying.
    // A killing blow can land twice in one swing, hence the guard.
    @Override
    public void onDeath()
    {
        if(dead)
            return;
        dead = true;
        getScene().setToDelete(getEntity());
        Engine.eventBus.publish(EnemyKilledEvent.class, e ->
        {
            e.enemy = enemy;
            e.room = home;
        });
        if(enemy.boss())
            Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = enemy.name() + " pokonany!");
    }

    private PlayerStats playerStats()
    {
        ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(playerEntity);
        if(scriptComponent != null && scriptComponent.script instanceof PlayerScript playerScript)
            return playerScript.getStats();
        return null;
    }

    private void setRandomTarget()
    {
        target.set(getComponent(TransformComponent.class).position).add(MathUtils.random(-maxMoveDistance,maxMoveDistance), MathUtils.random(-maxMoveDistance,maxMoveDistance));
    }
}
