package net.dp.rpg.engine.components;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;

public class PhysicalBodyComponent implements Component
{
    public Body body;

    public PhysicalBodyComponent(Body body)
    {
        this.body = body;
    }
}
