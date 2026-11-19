package net.dp.rpg.engine.components;

import com.badlogic.gdx.utils.OrderedMap;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.sprite.SpriteId;

public class MultiAnimationComponent implements Component
{
    public OrderedMap<SpriteId, AnimationSet<? extends AnimationId>> animationSets = new OrderedMap<>();

    public void add(AnimationSet<? extends AnimationId> animationSet, SpriteId id)
    {
        animationSets.put(id, animationSet);
    }
}
