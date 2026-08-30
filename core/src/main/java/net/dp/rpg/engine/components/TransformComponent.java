package net.dp.rpg.engine.components;

import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component
{
    public TransformComponent(Vector2 position, float rotation)
    {
        this.position = position;
        this.rotation = rotation;
    }

    public Vector2 position;
    public float rotation;
}
