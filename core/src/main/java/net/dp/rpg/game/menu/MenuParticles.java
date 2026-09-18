package net.dp.rpg.game.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ParticleEffectActor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;

public class MenuParticles extends Stack
{
    private final ParticleEffectActor particleActor;

    MenuParticles(ParticleEffect particle)
    {
        particleActor = new ParticleEffectActor(particle, true);
        particleActor.setTouchable(Touchable.disabled);
        particleActor.start();
        this.add(particleActor);
    }

    @Override
    public void layout()
    {
        particleActor.setPosition(0, Gdx.graphics.getHeight());
        particleActor.getEffect().getEmitters().first().getSpawnWidth().setHigh(Gdx.graphics.getWidth());
    }
}
