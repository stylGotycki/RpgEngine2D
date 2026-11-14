package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.AnimationComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.animations.EnemyAnimationId;
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
    private final Vector2 direction = new Vector2();
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
            if(!walking)
            {
                getComponent(AnimationComponent.class).animationSet.play(SpiderAnimationId.WALK);
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
}
