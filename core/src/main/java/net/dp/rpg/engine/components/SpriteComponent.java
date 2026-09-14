package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class SpriteComponent implements Component
{
    public Sprite sprite;

    public SpriteComponent(Sprite sprite)
    {
        this.sprite = sprite;
    }
}
