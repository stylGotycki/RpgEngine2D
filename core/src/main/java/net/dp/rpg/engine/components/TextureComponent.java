package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.Texture;

public class TextureComponent implements Component
{
    public TextureComponent(Texture texture)
    {
        this.texture = texture;
    }

    public Texture texture;
}
