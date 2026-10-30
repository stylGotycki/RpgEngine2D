package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class SpriteComponent implements Component
{
    public Sprite sprite;
    public boolean fixedRotation;

    public SpriteComponent(Sprite sprite, boolean fixedRotation)
    {
        this.sprite = sprite;
        this.fixedRotation = fixedRotation;
    }

    @Override
    public Component copy()
    {
        return new SpriteComponent(new Sprite(sprite), fixedRotation);
    }
}
