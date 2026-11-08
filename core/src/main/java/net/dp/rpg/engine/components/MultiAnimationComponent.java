package net.dp.rpg.engine.components;

import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;

public class MultiAnimationComponent implements Component
{
    public Array<AnimationSet<? extends AnimationId>> animationSets = new Array<>();

    public void add(AnimationSet<? extends AnimationId> animationSet)
    {
        animationSets.add(animationSet);
    }
}
