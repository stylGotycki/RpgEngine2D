package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.sprite.SpriteId;

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
                OrderedMap<SpriteId, AnimationSet<? extends AnimationId>> animations = animationComponents.getByIndex(i).animationSets;
                OrderedMap<SpriteId, SpriteComponent> sprites = spriteComponents.getByEntity(entity).sprites;
                ObjectMap.Keys<SpriteId> keys = sprites.keys();

                for(SpriteId key : keys)
                {
                    AnimationSet<? extends AnimationId> animation = animations.get(key);
                    if(animation != null)
                        sprites.get(key).sprite.setRegion(animation.getKeyFrame(delta));
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
