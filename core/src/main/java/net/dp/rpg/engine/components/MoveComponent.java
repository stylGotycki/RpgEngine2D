package net.dp.rpg.engine.components;

import com.badlogic.gdx.math.Vector2;

public class MoveComponent implements Component
{
    public Vector2 velocity;

    public MoveComponent(Vector2 velocity)
    {
        this.velocity = velocity;
    }
}
