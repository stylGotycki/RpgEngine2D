package net.dp.rpg.engine.components;

import com.badlogic.gdx.utils.OrderedMap;
import net.dp.rpg.engine.sprite.SpriteId;

public class MultiSpriteComponent implements Component
{
    public OrderedMap<SpriteId, SpriteComponent> sprites = new OrderedMap<>();

    public void add(SpriteComponent sprite, SpriteId id)
    {
        sprites.put(id, sprite);
    }
}
