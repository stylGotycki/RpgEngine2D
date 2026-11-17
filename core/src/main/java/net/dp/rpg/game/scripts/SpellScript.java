package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.PhysicalBodyComponent;

import static net.dp.rpg.game.FixtureCategory.*;

public class SpellScript extends EntityScript
{
    private Vector2 direction;
    private float speed;

    public SpellScript(float speed, Vector2 direction)
    {
        this.speed = speed;
        this.direction = direction;
    }

    @Override
    public void onStart()
    {
        Vector2 velocity = new Vector2().set(direction).scl(speed);
        getComponent(PhysicalBodyComponent.class).body.setLinearVelocity(velocity);
    }

    @Override
    public void update(float delta)
    {
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, ENEMY, WALL, OBSTACLE))
        {
            getScene().setToDelete(getEntity());
        }
    }
}
