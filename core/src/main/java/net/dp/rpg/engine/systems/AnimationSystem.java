package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.*;

public class AnimationSystem
{
    public void updateMultiAnimations(ComponentStorage<MultiSpriteComponent> spriteComponents, ComponentStorage<MultiAnimationComponent> animationComponents, float delta)
    {
        int aSize = animationComponents.size();
        for(int i = 0; i < aSize; i++)
        {
            int entity = animationComponents.getEntity(i);
            if(spriteComponents.hasComponent(entity))
            {
                Array<AnimationSet<? extends AnimationId>> animationSets = animationComponents.getByIndex(i).animationSets;
                Array<Sprite> spriteComponent = spriteComponents.getByEntity(entity).sprites;

                for(int j = 0; j < animationSets.size; j++)
                {
                    spriteComponent.get(j).setRegion(animationSets.get(j).getKeyFrame(delta));
                }
            }
        }
    }

    public void updateAnimations(ComponentStorage<SpriteComponent> spriteComponents, ComponentStorage<AnimationComponent> animationComponents, float delta)
    {
        int aSize = animationComponents.size();
        for(int i = 0; i < aSize; i++)
        {
            int entity = animationComponents.getEntity(i);
            if(spriteComponents.hasComponent(entity))
            {
                AnimationSet<? extends AnimationId> animationSet = animationComponents.getByIndex(i).animationSet;
                Sprite sprite = spriteComponents.getByEntity(entity).sprite;

                sprite.setRegion(animationSet.getKeyFrame(delta));
            }
        }
    }
}
