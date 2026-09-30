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
        Vector2 position = getScene().getComponentStorage(TransformComponent.class).getByEntity(getEntity()).position;
        direction.set(playerPosition).sub(position).setLength(1);
        getScene().getComponentStorage(TransformComponent.class).getByEntity(getEntity()).setRotation(direction.angleRad());

        Body body = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body;
        body.applyForce(direction.scl(speed), body.getPosition(), true);
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if((otherCategory & FixtureCategory.PLAYER_ATTACK) > 0)
        {
            HealthComponent health = getScene().getComponentStorage(HealthComponent.class).getByEntity(getEntity());
            health.dealDamage(20);
            Body body = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body;
            body.applyLinearImpulse(direction.scl(-1f), body.getPosition(), true);
            if(health.health <= 0)
                getScene().setToDelete(getEntity());
        }
    }
}
