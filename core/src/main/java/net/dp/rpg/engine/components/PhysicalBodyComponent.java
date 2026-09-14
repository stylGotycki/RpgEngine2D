package net.dp.rpg.engine.components;

import com.badlogic.gdx.physics.box2d.Body;

public class PhysicalBodyComponent implements Component
{
    public Body body;

    public PhysicalBodyComponent(Body body)
    {
        this.body = body;
    }
}
