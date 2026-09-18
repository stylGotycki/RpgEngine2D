package net.dp.rpg.game.menu;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ParticleButton extends Stack
{
    private final TextButton button;
    private final ParticleEffectActor particleActor;

    public ParticleButton(String name, ParticleEffect particle, Skin skin)
    {
        particleActor = new ParticleEffectActor(particle, false);
        particleActor.setTouchable(Touchable.disabled);

        button = new TextButton(name, skin);

        button.addListener(new ClickListener()
        {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor)
            {
                particleActor.setPosition(button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2);
                particleActor.getEffect().getEmitters().first().getSpawnWidth().setHigh(button.getWidth());
                particleActor.getEffect().getEmitters().first().getSpawnHeight().setHigh(button.getHeight());
                particleActor.start();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor)
            {
                particleActor.allowCompletion();
            }
        });

        this.add(button);
        this.add(particleActor);
    }

    @Override
    public void layout()
    {
        super.layout();
        particleActor.setPosition(button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2);
        particleActor.getEffect().getEmitters().first().getSpawnWidth().setHigh(button.getWidth());
        particleActor.getEffect().getEmitters().first().getSpawnHeight().setHigh(button.getHeight());

        //to prevent too many particles spawning after first enter
        particleActor.start();
        particleActor.allowCompletion();
    }
}
