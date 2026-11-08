package net.dp.rpg.engine.components;

import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;

public class AnimationComponent implements Component
{
    public AnimationSet<? extends AnimationId> animationSet;

    public AnimationComponent(AnimationSet<? extends AnimationId> animationSet)
    {
        this.animationSet = animationSet;
    }
}
