package net.dp.rpg.engine.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

public class AnimationSet<T extends AnimationId>
{
    private Animation<TextureRegion> currentAnimation;
    private final Map<T, Animation<TextureRegion>> animationMap = new HashMap<T, Animation<TextureRegion>>();
    private float currentTime = 0;

    public TextureRegion getKeyFrame(float deltaTime)
    {
        currentTime += deltaTime;
        if(currentAnimation != null)
            return currentAnimation.getKeyFrame(currentTime, true);
        return null;
    }

    public <T extends AnimationId> void play(T animationId)
    {
        currentTime = 0;
        if(animationMap.containsKey(animationId))
        {
            currentAnimation = animationMap.get(animationId);
        }
    }

    public void addAnimation(Animation<TextureRegion> newAnimation, T id)
    {
        animationMap.put(id, newAnimation);
        if(currentAnimation == null)
            currentAnimation = newAnimation;
    }
}
