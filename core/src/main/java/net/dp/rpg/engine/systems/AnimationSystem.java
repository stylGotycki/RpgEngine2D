package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.animation.AnimationId;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.MultiAnimationComponent;
import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.MultiSpriteComponent;

public class AnimationSystem
{
    public void updateSprites(ComponentStorage<MultiSpriteComponent> spriteComponents, ComponentStorage<MultiAnimationComponent> animationComponents, float delta)
    {
        int aSize = animationComponents.size();
        for(int i = 0; i < aSize; i++)
        {
            int entity = animationComponents.getEntity(i);
            if(spriteComponents.hasComponent(entity))
            {
                Array<AnimationSet<AnimationId>> animationSets = animationComponents.getByIndex(i).animationSets;
                Array<Sprite> spriteComponent = spriteComponents.getByEntity(entity).sprites;

                for(int j = 0; j < animationSets.size; j++)
                {
                    spriteComponent.get(j).setRegion(animationSets.get(j).getKeyFrame(delta));
                }
            }
        }
    }
}
