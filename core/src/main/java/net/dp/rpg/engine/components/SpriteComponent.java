package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class SpriteComponent implements Component
{
    public SpriteComponent(Sprite sprite)
    {
        this.sprite = sprite;
    }

    public Sprite sprite;
}
