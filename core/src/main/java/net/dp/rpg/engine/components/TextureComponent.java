package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class TextureComponent implements Component
{
    public TextureComponent(Texture texture)
    {
        this.texture = texture;
    }

    public Texture texture;
}
