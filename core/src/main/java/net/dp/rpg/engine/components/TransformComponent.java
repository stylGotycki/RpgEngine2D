package net.dp.rpg.engine.components;

import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component
{
    public Vector2 position;
    public float rotation;
    public float scale;

    public TransformComponent(Vector2 position, float rotation, float scale)
    {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    @Override
    public Component copy()
    {
        return new TransformComponent(new Vector2(position.x, position.y), rotation, scale);
    }
}
