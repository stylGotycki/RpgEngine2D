package net.dp.rpg.engine.components;

import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;

public class MultiAnimationComponent<T extends AnimationId> implements Component
{
    public Array<AnimationSet<T>> animationSets = new Array<>();

    public void add(AnimationSet<T> animationSet)
    {
        animationSets.add(animationSet);
    }
}
