package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.game.FixtureCategory;

public class EnemyScript extends EntityScript
{
    private final int playerEntity;
    private final Vector2 direction = new Vector2();

    public EnemyScript(int playerEntity)
    {
        this.playerEntity = playerEntity;
    }

    @Override
    public void update(float delta)
    {
        float speed = 10;
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
        if((otherCategory & FixtureCategory.PLAYER_ATTACK) > 0)
        {
            HealthComponent health = getComponent(HealthComponent.class);
            health.dealDamage(20);
            Body body = getComponent(PhysicalBodyComponent.class).body;
            body.applyLinearImpulse(direction.scl(-1f), body.getPosition(), true);
            if(health.health <= 0)
                getScene().setToDelete(getEntity());
        }
    }
}
