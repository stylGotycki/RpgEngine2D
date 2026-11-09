package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.events.EnemyKilledEvent;
import net.dp.rpg.game.events.GameMessageEvent;
import net.dp.rpg.game.item.PlayerStats;
import static net.dp.rpg.game.FixtureCategory.*;

public class EnemyScript extends EntityScript
{
    private final int playerEntity;
    private final Vector2 direction = new Vector2();
    private final TileFloor floor;
    private final TileRoom home;
    private final EnemyDef enemy;
    private boolean dead = false;

    public EnemyScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home)
    {
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
        PlayerStats playerStats = playerStats();
        if(playerStats != null && playerStats.isInvisible())
            return;

        // Acceleration, not force, so a heavy boss is not slower than its numbers say.
        float speed = enemy.acceleration() * getComponent(PhysicalBodyComponent.class).body.getMass();
        Vector2 playerPosition = getScene().getComponentStorage(TransformComponent.class).getByEntity(playerEntity).position;
        Vector2 position = getComponent(TransformComponent.class).position;
        direction.set(playerPosition).sub(position).setLength(1);
        getComponent(TransformComponent.class).setRotation(direction.angleRad());

        Body body = getComponent(PhysicalBodyComponent.class).body;
        body.applyForce(direction.scl(speed), body.getPosition(), true);
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, PLAYER_ATTACK))
        {
            HealthComponent health = getComponent(HealthComponent.class);
            PlayerStats playerStats = playerStats();
            health.dealDamage(playerStats == null ? 20 : playerStats.damage());
            Body body = getComponent(PhysicalBodyComponent.class).body;
            body.applyLinearImpulse(direction.scl(-1f), body.getPosition(), true);
            if(health.health <= 0 && !dead)
            {
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
        }
    }

    private PlayerStats playerStats()
    {
        ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(playerEntity);
        if(scriptComponent != null && scriptComponent.script instanceof PlayerScript playerScript)
            return playerScript.getStats();
        return null;
    }
}
