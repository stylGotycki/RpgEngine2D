package net.dp.rpg.engine.components;

import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component
{
    public Vector2 position;
    public float rotation;
    public float scale;
    public boolean changed = false;

    public TransformComponent(Vector2 position, float rotation, float scale)
    {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public void setPosition(Vector2 position)
    {
        this.position.set(position);
        changed = true;
    }

    public void setPosition(float x, float y)
    {
        position.set(x,y);
        changed = true;
    }

    public void setRotation(float rotation)
    {
        this.rotation = rotation;
        changed = true;
    }

    public void set(Vector2 position, float rotation)
    {
        this.position.set(position);
        this.rotation = rotation;
        changed = true;
    }

    @Override
    public Component copy()
    {
        return new TransformComponent(new Vector2(position.x, position.y), rotation, scale);
    }
}
