package net.dp.rpg.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.Script;
import net.dp.rpg.engine.components.PhysicalBodyComponent;

public class EtiScript extends Script
{
    private final Engine engine;

    public EtiScript(Engine engine)
    {
        this.engine = engine;
    }

    @Override
    public void update(float delta)
    {
        float speed = 3f;
        Vector2 direction = new Vector2(0,0);
        if(Gdx.input.isKeyPressed(Input.Keys.A))
        {
            direction.x -= 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D))
        {
            direction.x += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W))
        {
            direction.y += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S))
        {
            direction.y -= 1;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
            engine.exit();
        }

        direction.setLength(1);

        Body etiBody = getScene().getComponentStorage(PhysicalBodyComponent.class).getByIndex(getEntity()).body;
        etiBody.applyLinearImpulse(direction.scl(speed), etiBody.getPosition(), true);
    }
}
