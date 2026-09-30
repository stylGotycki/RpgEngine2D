package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;

public class SpriteComponent implements Component
{
    public Array<Sprite> sprites = new Array<>();
    public Array<Boolean> fixedRotation = new Array<>();

    public void add(Sprite sprite, boolean fixedRotation)
    {
        sprites.add(sprite);
        this.fixedRotation.add(fixedRotation);
    }

    @Override
    public Component copy()
    {
        SpriteComponent newComponent = new SpriteComponent();
        for(Sprite sprite : sprites)
        {
            newComponent.sprites.add(new Sprite(sprite));
        }
        return newComponent;
    }
}
